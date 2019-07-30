/*
 * java-gsissh.c
 *
 * author: Paul Hoover
 */

#include <assert.h>
#include <gssapi.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "com_sshtools_ssh_components_GssApi.h"
#include "error-handling.h"

typedef struct gss_context_struct {
	gss_OID oid;
	gss_name_t name;
	gss_ctx_id_t	context;
} gss_context;


/*
	convert_from_java_context
 */
static void convert_from_java_context(JNIEnv *env, jobject java_context, gss_context *native_context)
{
	assert(env);
	assert(java_context);
	assert(native_context);

	jclass context_class = (*env)->FindClass(env, "com/sshtools/ssh/components/GssApi$GssContext");
	jfieldID field_id = (*env)->GetFieldID(env, context_class, "oidAddr", "J");

	native_context->oid = (gss_OID)(*env)->GetLongField(env, java_context, field_id);

	field_id = (*env)->GetFieldID(env, context_class, "nameAddr", "J");

	native_context->name = (gss_name_t)(*env)->GetLongField(env, java_context, field_id);

	field_id = (*env)->GetFieldID(env, context_class, "contextAddr", "J");

	native_context->context = (gss_ctx_id_t)(*env)->GetLongField(env, java_context, field_id);
}

/*
	convert_to_java_context
 */
static void convert_to_java_context(JNIEnv *env, const gss_context *native_context, jobject java_context)
{
	assert(env);
	assert(native_context);
	assert(java_context);

	jclass context_class = (*env)->FindClass(env, "com/sshtools/ssh/components/GssApi$GssContext");
	jfieldID field_id = (*env)->GetFieldID(env, context_class, "oidAddr", "J");

	(*env)->SetLongField(env, java_context, field_id, (jlong)native_context->oid);

	field_id = (*env)->GetFieldID(env, context_class, "nameAddr", "J");

	(*env)->SetLongField(env, java_context, field_id, (jlong)native_context->name);

	field_id = (*env)->GetFieldID(env, context_class, "contextAddr", "J");

	(*env)->SetLongField(env, java_context, field_id, (jlong)native_context->context);
}

/*
	convert_from_java_oid
 */
static int convert_from_java_oid(JNIEnv *env, jobject java_oid, gss_OID native_oid)
{
	assert(env);
	assert(java_oid);
	assert(native_oid);

	jclass oid_class = (*env)->FindClass(env, "org/ietf/jgss/Oid");
	jmethodID method_id = (*env)->GetMethodID(env, oid_class, "getDER", "()[B");
	jbyteArray der = (*env)->CallObjectMethod(env, java_oid, method_id);
	jsize num_bytes = (*env)->GetArrayLength(env, der);
	jbyte *bytes = malloc(num_bytes);

	if (bytes == NULL) {
		throw_out_of_mem_error(env);

		return 1;
	}

	(*env)->GetByteArrayRegion(env, der, 0, num_bytes, bytes);

	native_oid->length = bytes[1];
	native_oid->elements = malloc(native_oid->length);

	if (native_oid->elements == NULL) {
		free(bytes);

		throw_out_of_mem_error(env);

		return 1;
	}

	memcpy(native_oid->elements, bytes + 2, native_oid->length);

	free(bytes);

	return 0;
}

/*
	create_init_reply
 */
static jobject create_init_reply(JNIEnv *env, OM_uint32 status, OM_uint32 flags, const gss_buffer_desc *token)
{
	assert(env);
	assert(token);

	jbyteArray java_token = (*env)->NewByteArray(env, token->length);

	if (java_token == NULL)
		return NULL;

	(*env)->SetByteArrayRegion(env, java_token, 0, token->length, (jbyte *)token->value);

	jclass result_class = (*env)->FindClass(env, "com/sshtools/ssh/components/GssApi$InitReply");
	jmethodID method_id = (*env)->GetMethodID(env, result_class, "<init>", "()V");
	jobject result = (*env)->NewObject(env, result_class, method_id);

	if (result == NULL)
		return NULL;

	jfieldID field_id = (*env)->GetFieldID(env, result_class, "status", "I");

	(*env)->SetIntField(env, result, field_id, status);

	field_id = (*env)->GetFieldID(env, result_class, "flags", "I");

	(*env)->SetIntField(env, result, field_id, flags);

	field_id = (*env)->GetFieldID(env, result_class, "token", "[B");

	(*env)->SetObjectField(env, result, field_id, java_token);

	return result;
}

/*
	initSecContext
 */
JNIEXPORT jobject JNICALL Java_com_sshtools_ssh_components_GssApi_initSecContext(JNIEnv *env, jclass caller, jobject context, jobject mech, jstring host, jbyteArray input)
{
	assert(env);

	if (context == NULL) {
		throw_null_pointer_error(env, "context");

		return NULL;
	}

	if (mech == NULL) {
		throw_null_pointer_error(env, "mech");

		return NULL;
	}

	if (host == NULL) {
		throw_null_pointer_error(env, "host");

		return NULL;
	}

	gss_context native_context;

	convert_from_java_context(env, context, &native_context);

	OM_uint32 major;
	OM_uint32 minor;

	if (native_context.oid == GSS_C_NO_OID) {
		native_context.oid = malloc(sizeof(gss_OID_desc));

		if (native_context.oid == NULL) {
			throw_out_of_mem_error(env);

			return NULL;
		}

		if (convert_from_java_oid(env, mech, native_context.oid) != 0) {
			free(native_context.oid);

			native_context.oid = GSS_C_NO_OID;

			return NULL;
		}
	}

	if (native_context.name == GSS_C_NO_NAME) {
		gss_buffer_desc buffer;
		const char *native_host = (*env)->GetStringUTFChars(env, host, NULL);
		int result = asprintf((char **)(&buffer.value), "host@%s", native_host);

		(*env)->ReleaseStringUTFChars(env, host, native_host);

		if (result < 0) {
			throw_out_of_mem_error(env);

			return NULL;
		}

		buffer.length = strlen((char *)buffer.value);

		major = gss_import_name(&minor, &buffer, GSS_C_NT_HOSTBASED_SERVICE, &native_context.name);

		free(buffer.value);

		if (major != GSS_S_COMPLETE) {
			throw_gssapi_error(env, major, minor, native_context.oid);

			return NULL;
		}
	}

	gss_buffer_desc *input_token;

	if (input != NULL) {
		input_token = malloc(sizeof(gss_buffer_desc));

		if (input_token == NULL) {
			throw_out_of_mem_error(env);

			return NULL;
		}

		input_token->length = (*env)->GetArrayLength(env, input);
		input_token->value = malloc(input_token->length);

		if (input_token->value == NULL) {
			free(input_token);

			throw_out_of_mem_error(env);

			return NULL;
		}

		(*env)->GetByteArrayRegion(env, input, 0, input_token->length, (jbyte *)input_token->value);
	}
	else
		input_token = GSS_C_NO_BUFFER;

	OM_uint32 request_flags = GSS_C_MUTUAL_FLAG | GSS_C_INTEG_FLAG | GSS_C_DELEG_FLAG;
	OM_uint32 return_flags;
	gss_buffer_desc output_token = GSS_C_EMPTY_BUFFER;

	major = gss_init_sec_context(&minor, GSS_C_NO_CREDENTIAL, &native_context.context, native_context.name, native_context.oid, request_flags, 0, NULL, input_token, NULL, &output_token, &return_flags, NULL);

	if (input_token != GSS_C_NO_BUFFER) {
		free(input_token->value);
		free(input_token);
	}

	jobject reply = create_init_reply(env, major, return_flags, &output_token);

	gss_release_buffer(&minor, &output_token);

	convert_to_java_context(env, &native_context, context);

	return reply;
}

/*
	verifyMic
 */
JNIEXPORT jboolean JNICALL Java_com_sshtools_ssh_components_GssApi_verifyMic(JNIEnv *env, jclass caller, jobject context, jbyteArray hash, jbyteArray mic)
{
	assert(env);

	if (context == NULL) {
		throw_null_pointer_error(env, "context");

		return JNI_FALSE;
	}

	if (hash == NULL) {
		throw_null_pointer_error(env, "hash");

		return JNI_FALSE;
	}

	if (mic == NULL) {
		throw_null_pointer_error(env, "mic");

		return JNI_FALSE;
	}

	gss_context native_context;

	convert_from_java_context(env, context, &native_context);

	gss_buffer_desc native_hash;

	native_hash.length = (*env)->GetArrayLength(env, hash);
	native_hash.value = malloc(native_hash.length);

	if (native_hash.value == NULL) {
		throw_out_of_mem_error(env);

		return JNI_FALSE;
	}

	(*env)->GetByteArrayRegion(env, hash, 0, native_hash.length, (jbyte *)native_hash.value);

	gss_buffer_desc native_mic;

	native_mic.length = (*env)->GetArrayLength(env, mic);
	native_mic.value = malloc(native_mic.length);

	if (native_mic.value == NULL) {
		free(native_hash.value);

		throw_out_of_mem_error(env);

		return JNI_FALSE;
	}

	(*env)->GetByteArrayRegion(env, mic, 0, native_mic.length, (jbyte *)native_mic.value);

	OM_uint32 major;
	OM_uint32 minor;

	major = gss_verify_mic(&minor, native_context.context, &native_hash, &native_mic, NULL);

	free(native_hash.value);
	free(native_mic.value);

	if (major == GSS_S_BAD_SIG)
		return JNI_FALSE;

	if (GSS_ERROR(major)) {
		throw_gssapi_error(env, major, minor, native_context.oid);

		return JNI_FALSE;
	}

	return JNI_TRUE;
}

/*
	getMic
 */
JNIEXPORT jbyteArray JNICALL Java_com_sshtools_ssh_components_GssApi_getMic(JNIEnv *env, jclass caller, jobject context, jbyteArray message)
{
	assert(env);

	if (context == NULL) {
		throw_null_pointer_error(env, "context");

		return NULL;
	}

	if (message == NULL) {
		throw_null_pointer_error(env, "message");

		return NULL;
	}

	gss_context native_context;

	convert_from_java_context(env, context, &native_context);

	gss_buffer_desc native_message;

	native_message.length = (*env)->GetArrayLength(env, message);
	native_message.value = malloc(native_message.length);

	if (native_message.value == NULL) {
		throw_out_of_mem_error(env);

		return NULL;
	}

	(*env)->GetByteArrayRegion(env, message, 0, native_message.length, (jbyte *)native_message.value);

	OM_uint32 major;
	OM_uint32 minor;
	gss_buffer_desc mic = GSS_C_EMPTY_BUFFER;

	major = gss_get_mic(&minor, native_context.context, GSS_C_QOP_DEFAULT, &native_message, &mic);

	free(native_message.value);

	if (major != GSS_S_COMPLETE) {
		gss_release_buffer(&minor, &mic);

		throw_gssapi_error(env, major, minor, native_context.oid);

		return NULL;
	}

	jbyteArray result = (*env)->NewByteArray(env, mic.length);

	if (result == NULL)
		return NULL;

	(*env)->SetByteArrayRegion(env, result, 0, mic.length, (jbyte *)mic.value);

	gss_release_buffer(&minor, &mic);

	return result;
}

/*
	deleteSecContext
 */
JNIEXPORT void JNICALL Java_com_sshtools_ssh_components_GssApi_deleteSecContext(JNIEnv *env, jclass caller, jobject context)
{
	assert(env);

	if (context == NULL) {
		throw_null_pointer_error(env, "context");

		return;
	}

	gss_context native_context;

	convert_from_java_context(env, context, &native_context);

	OM_uint32 minor;

	if (native_context.context != GSS_C_NO_CONTEXT) {
		gss_delete_sec_context(&minor, &native_context.context, GSS_C_NO_BUFFER);

		native_context.context = GSS_C_NO_CONTEXT;
	}

	if (native_context.oid != GSS_C_NO_OID) {
		free(native_context.oid->elements);
		free(native_context.oid);

		native_context.oid = GSS_C_NO_OID;
	}

	if (native_context.name != GSS_C_NO_NAME) {
		gss_release_name(&minor, &native_context.name);

		native_context.name = GSS_C_NO_NAME;
	}

	convert_to_java_context(env, &native_context, context);
}
