
#include <iostream>
#include <ctype.h>
#include <dirent.h>
#include <errno.h>
#include <limits.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/syscall.h> /* Definition of SYS_* constants */
#include <time.h>
#include <unistd.h>
#include <android/log.h>
#include "lmkd.h"
#include "liblmkd_utils.h"

#define TAG "termux-poll"

using namespace std;

#define loge(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static bool isnumeric(char* str)
{
    int i = 0;

    // Empty string is not numeric
    if (str[0] == 0)
        return false;

    while (1) {
        if (str[i] == 0) // End of string
            return true;

        if (isdigit(str[i]) == 0)
            return false;

        i++;
    }
}

void lmk(int pid){
    int retval;
    struct lmk_procprio params;
    params.pid = pid;
    //todo get uid from pid
    params.uid = getuid();
    params.oomadj = 1000;
    params.ptype = PROC_TYPE_APP;
    retval = lmkd_connect();
    loge("connect with lmkd: %i %s", retval,  strerror(errno));
    retval = lmkd_register_proc(retval, &params);
    loge("register with lmkd: %i %s", retval,  strerror(errno));
}

void memcg(){
	/*
    int retval = createProcessGroup(getuid(), getpid(), true);
    loge("could not create memcg: %i %s", retval, strerror (errno));
    */
}

extern "C" void ppoll(){
	loge("polling started");
	
	int pid = fork();
  	if (pid < 0) {
  	    loge("fork failed: %s\n", strerror(errno));
  	    return; 
  	}
  	else if (pid > 0) {
       loge("polling in: %i\n", pid);
       return;
    }
	
	
	while (1){
		DIR* procdir = opendir("/proc");
    if (procdir == NULL) {
        loge("%s: could not open /proc: %s", __func__, strerror(errno));
    }
    
	//procinfo_t victim = empty_procinfo;
    while (1) {
        errno = 0;
        struct dirent* d = readdir(procdir);
        if (d == NULL) {
            if (errno != 0)
                loge("%s: readdir error: %s", __func__, strerror(errno));
            break;
        }

        // proc contains lots of directories not related to processes,
        // skip them
        if (!isnumeric(d->d_name))
            continue;
                    
        int pid = (int)strtol(d->d_name, NULL, 10);    
        // todo get uid
        loge("adding %i to lmk",pid);
        lmk(pid);
        usleep(100*1000);
     }
     closedir(procdir);
     sleep(1);
     }
}

