#include <jni.h>
#include <fbjni/fbjni.h>
#include "NitroSharedWebviewOnLoad.hpp"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
  return facebook::jni::initialize(vm, []() {
    margelo::nitro::sharedwebview::registerAllNatives();
  });
}
