#include <jni.h>
#include "UserTime.h"

// some information for compiling, etc. can be found at: http://www-lehre.inf.uos.de/~rkunze/flashweather/Diplomarbeit/node31.html
// compiles for Mac OS X using: cc -dynamiclib -I/System/Library/Frameworks/JavaVM.framework/Headers/ -o libUserTime.jnilib de_jstacs_tools_UserTime.c

#ifdef __WIN32__ || _MSC_VER

#include <windows.h>
#include <stdio.h>

JNIEXPORT jfloat JNICALL Java_de_unihalle_informatik_MiToBo_tools_system_UserTime_getUserTime
  (JNIEnv *e, jobject obj)
{
          FILETIME ftCreation, ftExit, ftKernel, ftUser;
          GetProcessTimes(GetCurrentProcess(), &ftCreation, &ftExit, &ftKernel, &ftUser);
          return (jfloat) *((__int64 *) &ftUser);
}

JNIEXPORT jlong JNICALL Java_de_unihalle_informatik_MiToBo_tools_system_UserTime_getTicks
  (JNIEnv *e, jobject obj)
{
          return (jlong) 10000000;
}

#else

#include<sys/times.h>
#include<unistd.h>

JNIEXPORT jfloat JNICALL Java_de_unihalle_informatik_MiToBo_tools_system_UserTime_getUserTime
  (JNIEnv *e, jobject obj)
{
          struct tms x1;
          times(&x1);
          return x1.tms_utime;
}

JNIEXPORT jlong JNICALL Java_de_unihalle_informatik_MiToBo_tools_system_UserTime_getTicks
  (JNIEnv *e, jobject obj)
{
          return sysconf(_SC_CLK_TCK);
}
#endif
