/*
 * error-handling.c
 *
 * author: Paul Hoover
 */

#include <assert.h>
#include <ctype.h>
#include <errno.h>
#include <gssapi.h>
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "error-handling.h"

#define MAX_MESSAGE_LENGTH 1024

static const char *class_names[ ] = {
	"java/lang/RuntimeException",
	"java/lang/IllegalArgumentException",
	"java/lang/OutOfMemoryError",
	"java/lang/NullPointerException"
};


/*
	remove_newlines
 */
static void remove_newlines(char *value, size_t length)
{
	for (int i = 0 ; i < length ; i += 1) {
		if (value[i] == '\n')
			value[i] = ' ';
	}
}

/*
	throw_error
*/
void throw_error(JNIEnv *env, unsigned int name, const char *message)
{
	assert(env);
	assert(name < sizeof(class_names) / sizeof(char *));
	assert(message);

	jclass exception_class = (*env)->FindClass(env, class_names[name]);

	if (exception_class != NULL)
		(*env)->ThrowNew(env, exception_class, message);
}

/*
	throw_system_error
*/
void throw_system_error(JNIEnv *env)
{
	assert(env);

	int code = errno;

	if (code == ENOMEM) {
		throw_out_of_mem_error(env);

		return;
	}

	char *message = malloc(MAX_MESSAGE_LENGTH);

	if (message == NULL) {
		throw_out_of_mem_error(env);

		return;
	}

	strerror_r(code, message, MAX_MESSAGE_LENGTH);

	throw_error(env, JAVA_CLASS_RUNTIME_EXCEPT, message);

	free(message);
}

/*
	throw_out_of_mem_error
*/
void throw_out_of_mem_error(JNIEnv *env)
{
	assert(env);

	throw_error(env, JAVA_CLASS_OUT_OF_MEM, "Not enough memory");
}

/*
	throw_illegal_arg_error
*/
void throw_illegal_arg_error(JNIEnv *env, const char *name)
{
	assert(env);
	assert(name);

	throw_error(env, JAVA_CLASS_ILLEGAL_ARG, name);
}

/*
	throw_null_pointer_error
*/
void throw_null_pointer_error(JNIEnv *env, const char *name)
{
	assert(env);
	assert(name);

	throw_error(env, JAVA_CLASS_NULL_POINTER, name);
}

void throw_ssh_error(JNIEnv *env, const char *message)
{
	jclass exception_class = (*env)->FindClass(env, "com/sshtools/ssh/SshException");
	jfieldID field_id = (*env)->GetStaticFieldID(env, exception_class, "INTERNAL_ERROR", "I");
	jint code = (*env)->GetStaticIntField(env, exception_class, field_id);
	jstring java_message = (*env)->NewStringUTF(env, message);
	jmethodID method_id = (*env)->GetMethodID(env, exception_class, "<init>", "(Ljava/lang/String;I)V");
	jobject exception = (*env)->NewObject(env, exception_class, method_id, java_message, code);

	if (exception != NULL)
		(*env)->Throw(env, exception);
}

/*
	throw_gssapi_error
 */
void throw_gssapi_error(JNIEnv *env, OM_uint32 major, OM_uint32 minor, const gss_OID oid)
{
	assert(env);
	assert(oid);

	OM_uint32 status;
	OM_uint32 context = 0;
	gss_buffer_desc buffer = GSS_C_EMPTY_BUFFER;

	gss_display_status(&status, major, GSS_C_GSS_CODE, oid, &context, &buffer);

	remove_newlines((char *)buffer.value, buffer.length);

	char *message = malloc(buffer.length);

	if (message == NULL) {
		gss_release_buffer(&status, &buffer);

		throw_out_of_mem_error(env);

		return;
	}

	memcpy(message, buffer.value, buffer.length);

	gss_release_buffer(&status, &buffer);

	while (context != 0) {
		gss_display_status(&status, major, GSS_C_GSS_CODE, oid, &context, &buffer);

		remove_newlines((char *)buffer.value, buffer.length);

		char *value;
		int result = asprintf(&value, "%s; %s", message, (char *)buffer.value);

		free(message);
		gss_release_buffer(&status, &buffer);

		if (result < 0) {
			throw_out_of_mem_error(env);

			return;
		}

		message = value;
	}

	do {
		gss_display_status(&status, minor, GSS_C_MECH_CODE, oid, &context, &buffer);

		remove_newlines((char *)buffer.value, buffer.length);

		char *value;
		int result = asprintf(&value, "%s; %s", message, (char *)buffer.value);

		free(message);
		gss_release_buffer(&status, &buffer);

		if (result < 0) {
			throw_out_of_mem_error(env);

			return;
		}

		message = value;
	} while (context != 0);

	throw_ssh_error(env, message);

	free(message);
}
