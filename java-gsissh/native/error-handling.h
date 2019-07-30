/*
 * error-handling.h
 *
 * author: Paul Hoover
 */

#if !defined(ERROR_HANDLING_H)
#define ERROR_HANDLING_H


enum java_class_names {
	JAVA_CLASS_RUNTIME_EXCEPT,
	JAVA_CLASS_ILLEGAL_ARG,
	JAVA_CLASS_OUT_OF_MEM,
	JAVA_CLASS_NULL_POINTER
};

extern void throw_error(JNIEnv *env, unsigned int name, const char *message);
extern void throw_system_error(JNIEnv *env);
extern void throw_out_of_mem_error(JNIEnv *env);
extern void throw_illegal_arg_error(JNIEnv *env, const char *name);
extern void throw_null_pointer_error(JNIEnv *env, const char *name);
extern void throw_ssh_error(JNIEnv *env, const char *message);
extern void throw_gssapi_error(JNIEnv *env, OM_uint32 major, OM_uint32 minor, const gss_OID oid);


#endif /* ERROR_HANDLING_H */
