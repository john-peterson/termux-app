# Build termux

Be sure to clone recursively otherwise submodules won't be downloaded. If earlyoom is missing you cloned without submodules. Run this inside an already cloned repo to download submodules 

```
git submodule update --init --recursive
```

## IA-32 host

These instructions are for Ubuntu IA-32 build host. As of 2024 the Android Gradle plugin supports only arm targets not hosts. it will download and run IA-32 programs on ARM hosts and the build will fail in Termux and all other ARM systems 

```
sudo apt install -y openjdk-17-jdk unzip file git

export adk=$HOME/adk
export ANDROID_HOME=$adk
export PATH=$PATH:$adk/cmdline-tools/bin

wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip command*
mkdir $adk; mv cmd* $adk/
yes|sdkmanager --sdk_root=$adk --licenses

git clone --recursive --depth 1 --shallow-submodules https://github.com/KitsunedFox/termux-monet
./gradlew assembleDebug
find -name "*.apk"
```

If you only have a phone the only solution is to run an x86 shell in termux and copy the apk back to your phone 

```
gcloud compute instances create u --provisioning-model spot --scopes https://www.googleapis.com/auth/devstorage.full_control  --image-project ubuntu-os-cloud --image-family ubuntu-minimal-2404-lts-amd64 --machine-type n1-standard-4
gsutil cp apk gs://...
```

## ARM host 

These instructions need to be updated by someone that has aced this procedure. It should be possible to overwrite the x86 programs with these arm versions 

```
https://github.com/AndroidIDEOfficial/androidide-tools/releases/download/v34.0.4/build-tools-34.0.4-aarch64.tar.xz

https://github.com/lzhiyong/termux-ndk/releases/download/android-ndk/android-ndk-r26b-aarch64.zip
```

It might also be possible to override the gradle paths. This is enough to build the java files but the clang path has to be changed too for the C files

```
nano ~/.gradle/gradle.properties
android.aapt2FromMavenOverride=/data/data/com.termux/files/home/adk/build-tools/34.0.4/aapt2
```

### Data limit warning 

Gradle might end up downloading several different ndk versions that are around 0,7/2,3 gig compressed/uncompressed each and still fail. If you don't have wifi it's best to practice in the cloud first. This will give you a Ubuntu ARM shell inside termux for minimal cost. If you manage to build it there the same steps will work in termux 

```
gcloud compute instances create ua --image-project ubuntu-os-cloud --image-family ubuntu-minimal-2404-lts-arm64 --machine-type t2a-standard-1 --zone europe-west4-a --provisioning-model spot --scopes https://www.googleapis.com/auth/devstorage.full_control 
gcloud compute ssh a@ua
```

# Out Of Memory OOM when building termux

If Termux doesn't build on your phone you can try this to reduce memory use

```
nano ~/.gradle/gradle.properties
org.gradle.parallel=false
org.gradle.daemon=false
org.gradle.jvmargs=-Xmx256m
```

If the build dies without reporting errors run this to learn if the memory manager reaped the java process that gradle is running in

```
logcat -d|grep oom
```

