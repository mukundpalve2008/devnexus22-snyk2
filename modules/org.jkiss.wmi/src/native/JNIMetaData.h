#pragma once

#ifndef _JNI_META_DATA_H_
#define _JNI_META_DATA_H_

#include <jni.h>

static const char* CLASS_WMI_SERVICE = "org/jkiss/wmi/service/WMIService";
static const char* CLASS_WMI_OBJECT = "org/jkiss/wmi/service/WMIObject";
static const char* CLASS_WMI_OBJECT_SINK = "org/jkiss/wmi/service/WMIObjectSink";
static const char* CLASS_WMI_OBJECT_SINK_STATUS = "org/jkiss/wmi/service/WMIObjectSinkStatus";
static const char* CLASS_WMI_OBJECT_PROPERTY = "org/jkiss/wmi/service/WMIObjectProperty";
static const char* CLASS_WMI_OBJECT_METHOD = "org/jkiss/wmi/service/WMIObjectMethod";

class JNIMetaData
{
public:
	JNIMetaData(JNIEnv* pEnv);
	~JNIMetaData();

	static JNIMetaData& GetMetaData(JNIEnv* pEnv);
	//static void Destroy();

	JNIEnv* pJavaEnv;

	jclass wmiServiceClass;
	jmethodID wmiServiceConstructor;
	jfieldID wmiServiceHandleField;
	jfieldID wmiServiceLogField;

	jclass wmiObjectClass;
	jmethodID wmiObjectConstructor;
	jfieldID wmiObjectHandleField;

	jclass wmiObjectSinkClass;
	jmethodID wmiObjectSinkIndicateMethod;
	jmethodID wmiObjectSinkSetStatusMethod;
	jclass wmiObjectSinkStatusClass;
	jclass wmiObjectPropertyClass;
	jmethodID wmiObjectPropertyConstructor;
	jclass wmiObjectMethodClass;
	jmethodID wmiObjectMethodConstructor;

	jclass javaLangObjectClass;
	jclass javaLangByteClass;
	jclass javaLangCharClass;
	jclass javaLangBooleanClass;
	jclass javaLangShortClass;
	jclass javaLangIntegerClass;
	jclass javaLangLongClass;
	jclass javaLangFloatClass;
	jclass javaLangDoubleClass;
	jclass javaLangStringClass;
	jclass javaUtilDateClass;
	jclass javaUtilListClass;

	jmethodID javaLangByteConstructor;
	jmethodID javaLangCharConstructor;
	jmethodID javaLangBooleanConstructor;
	jmethodID javaLangShortConstructor;
	jmethodID javaLangIntegerConstructor;
	jmethodID javaLangLongConstructor;
	jmethodID javaLangFloatConstructor;
	jmethodID javaLangDoubleConstructor;
	jmethodID javaUtilDateConstructor;
	jmethodID javaUtilListAddMethod;
	jmethodID javaLangObjectEqualsMethod;

private:
	jclass FindJavaClass(const char* className);
	jmethodID FindJavaMethod(jclass clazz, const char* methodName, const char* methodSig);
	void DeleteClassRef(jclass& clazz);

	static JNIMetaData* instance;
};

#endif