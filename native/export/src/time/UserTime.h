#include <jni.h>

#ifndef USER_TILE
#define USER_TIME
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     de_unihalle_informatik_MiToBo_tools_system_UserTime
 * Method:    getUserTime
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_de_unihalle_informatik_MiToBo_tools_system_UserTime_getUserTime
  (JNIEnv *, jobject);

/*
 * Class:     de_unihalle_informatik_MiToBo_tools_system_UserTime
 * Method:    getTicks
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_de_unihalle_informatik_MiToBo_tools_system_UserTime_getTicks
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
