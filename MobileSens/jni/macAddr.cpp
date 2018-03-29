#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "macAddr.h"
#include <unistd.h>

/*
 * 获取WifiManager 对象
 * 参数： jCtxObj 为Context对象
 */
jobject getWifiManagerObj(JNIEnv *env, jclass clz, jobject jCtxObj)
{
	LOGI("gotWifiMangerObj ");
	//获取 Context.WIFI_SERVICE 的值
	//jstring  jstr_wifi_serveice = env->NewStringUTF("wifi");
	jclass jCtxClz= env->FindClass("android/content/Context");
	jfieldID fid_wifi_service = env->GetStaticFieldID(jCtxClz,"WIFI_SERVICE","Ljava/lang/String;");
	jstring  jstr_wifi_serveice = (jstring)env->GetStaticObjectField(jCtxClz,fid_wifi_service);

	jclass jclz = env->GetObjectClass(jCtxObj);
	jmethodID  mid_getSystemService = env->GetMethodID(jclz,"getSystemService","(Ljava/lang/String;)Ljava/lang/Object;");
	jobject wifiManager = env->CallObjectMethod(jCtxObj,mid_getSystemService,jstr_wifi_serveice);

	//因为jclass 继承自 jobject，所以需要释放；
	//jfieldID、jmethodID是内存地址，这段内存也不是在我们代码中分配的，不需要我们来释放。
	env->DeleteLocalRef(jCtxClz);
	env->DeleteLocalRef(jclz);
	env->DeleteLocalRef(jstr_wifi_serveice);

	return wifiManager;
}
/*
 * 获取WifiInfo 对象
 * 参数： wifiMgrObj 为WifiManager对象
 */
jobject getWifiInfoObj(JNIEnv *env, jobject wifiMgrObj)
{
	LOGI("getWifiInfoObj ");
	if(wifiMgrObj == NULL){
		return NULL;	
	}
	jclass jclz = env->GetObjectClass(wifiMgrObj);
	jmethodID mid = env->GetMethodID(jclz,"getConnectionInfo","()Landroid/net/wifi/WifiInfo;");
	jobject wifiInfo = env->CallObjectMethod(wifiMgrObj,mid);

	env->DeleteLocalRef(jclz);
	return wifiInfo;
}

/*
 * 获取MAC地址
 * 参数：wifiInfoObj， WifiInfo的对象
 */
char* getMacAddress(JNIEnv *env, jobject wifiInfoObj)
{
	LOGI("getMacAddress.... ");
	if(wifiInfoObj == NULL){
		return NULL;
	}
	jclass jclz = env->GetObjectClass(wifiInfoObj);
	jmethodID mid = env->GetMethodID(jclz,"getMacAddress","()Ljava/lang/String;");
	jstring jstr_mac = (jstring)env->CallObjectMethod(wifiInfoObj,mid);
	if(jstr_mac == NULL){
		env->DeleteLocalRef(jclz);
		return NULL;
	}

	const char* tmp = env->GetStringUTFChars(jstr_mac, NULL);
	char* mac = (char*) malloc(strlen(tmp)+1);
	memcpy(mac,tmp,strlen(tmp)+1);
	env->ReleaseStringUTFChars(jstr_mac, tmp);
	env->DeleteLocalRef(jclz);
	return mac;
}

bool enableWifi(JNIEnv *env, jobject wifiMgrObj)
{
	bool ret = false;
	jclass jclz = env->GetObjectClass(wifiMgrObj);
	jmethodID mid = env->GetMethodID(jclz,"getWifiState","()I");
	int wifiState = (int)env->CallIntMethod(wifiMgrObj,mid);
	LOGI("enableWifi wifiState: %d", wifiState);

	jfieldID fid = env->GetStaticFieldID(jclz,"WIFI_STATE_DISABLED","I");
	int wifiState_disabled = env->GetStaticIntField(jclz,fid);
	fid = env->GetStaticFieldID(jclz,"WIFI_STATE_DISABLING","I");
	int wifiState_disabling = env->GetStaticIntField(jclz,fid);
	if(wifiState==wifiState_disabled || wifiState==wifiState_disabling){
		mid =  env->GetMethodID(jclz,"setWifiEnabled","(Z)Z");
		ret = env->CallBooleanMethod(wifiMgrObj,mid,true);
	}
	env->DeleteLocalRef(jclz);
	return ret;
}


bool disableWifi(JNIEnv *env, jobject wifiMgrObj)
{
	bool ret = false;
	jclass jclz = env->GetObjectClass(wifiMgrObj);
	jmethodID mid = env->GetMethodID(jclz,"getWifiState","()I");
	int wifiState = (int)env->CallIntMethod(wifiMgrObj,mid);
	LOGI("disableWifi wifiState: %d", wifiState);

	jfieldID fid = env->GetStaticFieldID(jclz,"WIFI_STATE_ENABLED","I");
	int wifiState_enabled = env->GetStaticIntField(jclz,fid);
	fid = env->GetStaticFieldID(jclz,"WIFI_STATE_ENABLING","I");
	int wifiState_enabling = env->GetStaticIntField(jclz,fid);

	if(wifiState==wifiState_enabled || wifiState==wifiState_enabling){
		mid =  env->GetMethodID(jclz,"setWifiEnabled","(Z)Z");
		ret = env->CallBooleanMethod(wifiMgrObj,mid,false);
	}
	env->DeleteLocalRef(jclz);
	return ret;
}

/*
 * Class:     com_nevin_dmeo_jnimac_jni_MacAddr
 * Method:    getMacAddr
 * Signature: (Landroid/content/Context;)Ljava/lang/String;
 */
jstring JNI_FUNC(getMacAddr)(JNIEnv *env, jclass clz, jobject jCtxObj)
{
	jobject wifiManagerObj = NULL ;
	jobject wifiInfoObj = NULL;
	LOGI("getDeviceInfo......");
	wifiManagerObj = getWifiManagerObj(env, clz, jCtxObj);
	wifiInfoObj = getWifiInfoObj(env,wifiManagerObj);
	char * mac = getMacAddress(env,wifiInfoObj);
	//如果手机从开机之后就没有用过wifi，那么mac地址将为空，
	//下面的这一段代码就是用来解决这个问题的。
	if(mac == NULL){
		enableWifi(env,wifiManagerObj);
		for(int i=0; i<10 && mac==NULL; i++){
			sleep(1);
			env->DeleteLocalRef(wifiInfoObj);
			wifiInfoObj = getWifiInfoObj(env,wifiManagerObj);
			mac = getMacAddress(env,wifiInfoObj);
			LOGI("%d, mac= %s",i,mac);
		}
		disableWifi(env,wifiManagerObj);
	}
	//-----------------------------------------
	LOGI("mac: %s",mac);
	jstring result;
	if(mac != NULL){
		result = env->NewStringUTF(mac);
		free(mac);
	}else{
		result = env->NewStringUTF("");
	}
	env->DeleteLocalRef(wifiInfoObj);
	env->DeleteLocalRef(wifiManagerObj);
	return result;
}

static JNINativeMethod gMethods[] = {
		{"getMacAddr", "(Landroid/content/Context;)Ljava/lang/String;", (void*)JNI_FUNC(getMacAddr)},
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className,JNINativeMethod* gMethods, int numMethods)
{
	jclass clazz;
	clazz = env->FindClass(className);
	if (clazz == NULL) {
		LOGE("Native registration unable to find class '%s'", className);
		return JNI_FALSE;
	}
	if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
		LOGE("RegisterNatives failed for '%s'", className);
		env->DeleteLocalRef(clazz);
		return JNI_FALSE;
	}
	env->DeleteLocalRef(clazz);
	return JNI_TRUE;
}


jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv *env;
	LOGI("JNI_OnLoad!");
	if ( vm->GetEnv((void **) &env, JNI_VERSION_1_4) ){
		LOGE("ERROR: GetEnv failed");
		return JNI_ERR;
	}

	if (registerNativeMethods(env, CLASS_PATH_NAME, gMethods, sizeof(gMethods)/sizeof(gMethods[0])) != JNI_TRUE) {
		LOGE("ERROR: registerNatives failed");
		return JNI_ERR;
	}
	return JNI_VERSION_1_4;
}

void JNI_OnUnload(JavaVM* vm, void* reserved)
{
	JNIEnv *env;
	jclass cls;
	LOGI("JNI_OnUnLoad!");
	if (vm->GetEnv((void **) &env, JNI_VERSION_1_4))
		return;

	cls = env->FindClass(CLASS_PATH_NAME);
	env->UnregisterNatives(cls);

	return;
}
