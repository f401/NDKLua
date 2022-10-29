/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class net_fred_lua_jni_CStandardOutputInput */

#ifndef _Included_net_fred_lua_jni_CStandardOutputInput
#define _Included_net_fred_lua_jni_CStandardOutputInput
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     net_fred_lua_jni_CStandardOutputInput
 * Method:    getStandardOutPointer
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_net_fred_lua_jni_CStandardOutputInput_getStandardOutPointer
  (JNIEnv *, jobject);

/*
 * Class:     net_fred_lua_jni_CStandardOutputInput
 * Method:    getStandardErrPointer
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_net_fred_lua_jni_CStandardOutputInput_getStandardErrPointer
  (JNIEnv *, jobject);

/*
 * Class:     net_fred_lua_jni_CStandardOutputInput
 * Method:    getStandardInPointer
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_net_fred_lua_jni_CStandardOutputInput_getStandardInPointer
  (JNIEnv *, jobject);

/*
 * Class:     net_fred_lua_jni_CStandardOutputInput
 * Method:    setStandardOutPointer
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_net_fred_lua_jni_CStandardOutputInput_setStandardOutPointer
  (JNIEnv *, jobject, jlong);

/*
 * Class:     net_fred_lua_jni_CStandardOutputInput
 * Method:    setStandardErrPointer
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_net_fred_lua_jni_CStandardOutputInput_setStandardErrPointer
  (JNIEnv *, jobject, jlong);

/*
 * Class:     net_fred_lua_jni_CStandardOutputInput
 * Method:    setStandardInPointer
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_net_fred_lua_jni_CStandardOutputInput_setStandardInPointer
  (JNIEnv *, jobject, jlong);

/*
 * Class:     net_fred_lua_jni_CStandardOutputInput
 * Method:    redirectStandardOutTo
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_net_fred_lua_jni_CStandardOutputInput_redirectStandardOutTo
  (JNIEnv *, jobject, jstring);

/*
 * Class:     net_fred_lua_jni_CStandardOutputInput
 * Method:    redirectStandardErrTo
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_net_fred_lua_jni_CStandardOutputInput_redirectStandardErrTo
  (JNIEnv *, jobject, jstring);

/*
 * Class:     net_fred_lua_jni_CStandardOutputInput
 * Method:    redirectStandardInTo
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_net_fred_lua_jni_CStandardOutputInput_redirectStandardInTo
  (JNIEnv *, jobject, jstring);

#ifdef __cplusplus
}
#endif
#endif
