#
# Makefile for building the Reporter Manager
#

include etc/common/Makefile.perl.common

cleanSchedule:
	sched=$(PREFIX)/var/schedule.xml; \
  if ( test -f $$sched ); then \
    rm -f $$sched; \
  fi
