##############################################################################
#
# Simple installer script for the Inca Test Harness and Reporting Framework
#
##############################################################################

#----------------------------------------------------------------------------#
# Global variables
#----------------------------------------------------------------------------#

include Makefile.inc

LOG="$(TOP_DIR)/make.log"

MODULES = reporter_manager

PERL_PKGS = manager \
            util/perl

PYTHON_PKGS = reporters/lib/python

JAVA_PKGS = support/apache-ant harness/depot

MAKE_PKGS = harness/publish

AUTOCONF_PKGS = support/expat samples/teragrid 

CONFIG_PKGS = setup/etc reporters/bin

DIRS = ${INCA_LOCATION} ${INCA_LOCATION}/bin ${INCA_LOCATION}/etc ${INCA_LOCATION}/var ${INCA_LOCATION}/libexec ${INCA_LOCATION}/samples ${INCA_LOCATION}/docs

#----------------------------------------------------------------------------#
# configuration
#----------------------------------------------------------------------------#

# don't print out commands 
.SILENT:

# treat the following as targets
.PHONY: ${PERL_PKGS} ${PYTHON_PKGS} ${JAVA_PKGS} ${MAKE_PKGS} ${CONFIG_PKGS} ${MODULES} ${AUTOCONF_PKGS} 

#----------------------------------------------------------------------------#
# dummy targets
#----------------------------------------------------------------------------#

# default target
all: dirs ${MODULES} 

dirs: ${DIRS}

reporter_manager_module:  util/perl manager 

#----------------------------------------------------------------------------#
# build, clean, and install targets
#----------------------------------------------------------------------------#

${MODULES}:
	echo ""; \
	echo "Installing module '$@'"; \
	echo ""; \
	${MAKE} $@_module; \
	echo ""; \
	echo "done making module '$@'"; \
	echo ""; \

${PERL_PKGS}:
	if ( test -d $@ ); then \
	  echo "Entering $@..."; \
	  cd $@; \
	  echo "  configure"; \
	      perl -I${INCA_LOCATION}/lib/perl Makefile.PL INSTALLDIRS=perl \
	  		     LIB=${INCA_LOCATION}/lib/perl \
	                       INSTALLSCRIPT=${INCA_LOCATION}/bin \
	  	    	     INSTALLMAN1DIR=${INCA_LOCATION}/man/man1 \
	  		     INSTALLMAN3DIR=${INCA_LOCATION}/man/man3 \
                             EXPATLIBPATH=${INCA_LOCATION}/lib \
                             EXPATINCPATH=${INCA_LOCATION}/include \
	  													 --noprompt \
	  		     >> ${LOG}  2>&1; \
	  echo "  ${MAKE}"; \
	  $(MAKE) >> ${LOG} 2>&1; \
	  echo "  ${MAKE} install"; \
	  $(MAKE) install >> ${LOG} 2>&1; \
	  echo "  done"; \
	fi

${PYTHON_PKGS}:
	if ( test -d $@ ); then \
	  echo "Entering $@..."; \
	  cd $@; \
	  if ( test -z "${PYTHONPATH}" ); then \
	    PYTHONPATH=${PYTHONPATH}:${INCA_LOCATION}/lib/python; \
        else \
	    PYTHONPATH=${INCA_LOCATION}/lib/python; \
 	fi;\
	  echo "  setup.py install"; \
	  env PYTHONPATH=${PYTHONPATH} \
	      python setup.py install --home=${INCA_LOCATION} >> ${LOG} 2>&1; \
	  echo "  done"; \
	fi

${AUTOCONF_PKGS}:
	if ( test -d $@ ); then \
	  echo "Entering $@..."; \
	  cd $@; \
	  echo "  configure"; \
	  ./configure --prefix=${INCA_LOCATION} --disable-shared >> ${LOG} 2>&1; \
	  echo "  ${MAKE}"; \
	  $(MAKE) >> ${LOG} 2>&1; \
	  echo "  ${MAKE} install"; \
	  $(MAKE) install >> ${LOG} 2>&1; \
	  echo "  done"; \
	fi

reporters/bin:
	if ( test -d $@ ); then \
	  echo "Entering $@..."; \
	  cd $@; \
	  cp * ${INCA_LOCATION}/bin; \
	  chmod a+x ${INCA_LOCATION}/bin; \
	  echo "  done"; \
	fi

harness/depot:  support/apache-ant support/Getopt-Tabular support/SOAP-Lite
	if ( test -z "${PATH}" ); then \
	  PATH=${INCA_LOCATION}/bin; \
        else \
	  PATH=${PATH}:${INCA_LOCATION}/bin; \
	fi;\
	if ( test -d $@ ); then \
	  echo "Entering $@..."; \
	  cd $@; \
	  echo "  ant install-depot (this may take a long time)"; \
	  ant -DINCA_SRC=${TOP_DIR} -DINCA_LOCATION=${INCA_LOCATION} install-depot  >> ${LOG} 2>&1; \
	  echo "  done"; \
	fi

support/apache-ant:
	if ( test -d $@ ); then \
	  echo "Entering $@..."; \
	  cd $@; \
	  echo '  cp bin/* $$INCA_LOCATION'; \
	  cp $(TOP_DIR)/support/apache-ant/bin/* ${INCA_LOCATION}/bin >> ${LOG} 2>&1; \
	  echo '  cp lib/* $$INCA_LOCATION'; \
	  cp $(TOP_DIR)/support/apache-ant/lib/*.jar ${INCA_LOCATION}/lib >> ${LOG} 2>&1; \
	  echo "  done"; \
	fi

${MAKE_PKGS}:
	if ( test -d $@ ); then \
	  echo "Entering $@..."; \
	  cd $@; \
	  echo "  ${MAKE}"; \
	  ${MAKE} >> ${LOG} 2>&1; \
	  echo "  ${MAKE} install"; \
	  ${MAKE} install >> ${LOG} 2>&1; \
	  echo "  done"; \
	fi
	
${DIRS}:
	echo;
	echo "Create $@";
	mkdir $@

setup/etc: 
	echo "Entering $@..."; \
	echo '  cp $@/*.sh $$INCA_LOCATION/etc'; \
	cp $@/*sh ${INCA_LOCATION}/etc

docs/talks/overview:
	echo "cp docs/talks/overview/reporter-tutorial.[ph]* ${INCA_LOCATION}/docs"
	cp docs/talks/overview/reporter-tutorial.[ph]* ${INCA_LOCATION}/docs

docs/guides:
	echo "cp docs/guides/[ir]*.[ph]* ${INCA_LOCATION}/docs"
	cp docs/guides/[ir]*.[ph]* ${INCA_LOCATION}/docs

samples/simple:
	if ( test -d $@ ); then \
	  echo "Entering $@..."; \
	  echo '  cp $@/*.xml $$INCA_LOCATION/samples'; \
	  cp $@/*.xml ${INCA_LOCATION}/samples; \
	fi

distclean:
	if ( test -d $@ ); then \
	  for dir in $(PERL_PKGS); do \
	    echo "Entering $${dir}..."; \
          cd $${dir}; \
	    echo "  ${MAKE} distclean"; \
	    ${MAKE} distclean >> ${LOG} 2>&1; \
	    cd $(TOP_DIR); \
	    echo "  done"; \
	  done; \
	fi

