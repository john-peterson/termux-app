#include <stdio.h>
#include <string.h>
#include <sys/wait.h>
#include <unistd.h>
#include <sys/prctl.h>
#include <kill.h>
#include <android/log.h>

#define LOG_TAG "termux-oom"

#define loge(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern char *__progname;
void name(char *n) {
    prctl(PR_SET_NAME, n);
    strncpy (__progname, n, strlen(__progname));
}
    
extern int enable_debug;
extern void poll_loop(const poll_loop_args_t* args);
extern void startup_selftests(poll_loop_args_t* args);

void earlyoom() {
    if (fork() > 0) return;
    loge("earlyoom pid %i\n", getpid());
    name("earlyoom");
    
    //setenv("NO_COLOR", 1, 1);
    enable_debug = 0;
	
    poll_loop_args_t args = {
    	//act before lmk kill termux
        .mem_term_percent = 20,
        .swap_term_percent = 20,
        .mem_kill_percent = 10,
        .swap_kill_percent = 10,
    //    .report_interval_ms = 1000*60,
        .report_change_pc = 20,
        .ignore_root_user = 1,
        .sort_by_rss = 1,
        .ignore_regex = "termux",
        .notify = 0,
    //    .notify_ext = echo kill abc>/dev/pts/#
        .kill_process_group = 0,
    };
    
    earlyoom_syslog_init();
    startup_selftests(&args);
    poll_loop(&args);
}

bool oom_enabled = 0;
void oom(char *argv) {
    if (oom_enabled) return;
	oom_enabled = 1;
	
	loge("oom parent %i\n", getpid());
    if (fork() > 0) return;

    loge("oom thread %i\n", getpid());
    name("oom");
    stdout2logcat();
    
earlyoom: 
	earlyoom();
	wait (0);
	sleep(10);
	loge("restarting earlyoom");
goto earlyoom;
}