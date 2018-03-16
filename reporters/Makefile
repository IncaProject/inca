include version
include etc/common/Makefile.common

BIN_FILES=bin/*
CGI_FILES=cgi-bin/*
CHMOD=chmod
COMPRESS=gzip --best
CP=cp
DIST_TEMP=$(NAME)-DIST
DOC_DIR=docs
ETC_FILES=etc/*
DEPEND=TODO
FIXIN=true
LOG=`pwd`/build.log
MKDIR=mkdir
MKDIR_P=mkdir -p
MV=mv
NAME=Inca-Reporter
NOECHO=@
PERL_FILES=$(BIN_FILES) $(CGI_FILES) $(SBIN_FILES)
PERM_RW=+rw
PERM_RWX=+rwx
PREFIX=/usr/local
RM_F=rm -f
RM_RF=rm -fr
SBIN_FILES=sbin/*
TAR=tar
TARFLAGS= cvf

Packages.gz:
	rm -f lib/python/inca/*.pyc
	sbin/incpack -I lib/perl -I lib/python lib/perl/Inca lib/python/inca bin share -X share/teragrid-certs.tar.gz

clean-doc:
	rm -fr $(DOC_DIR)

clean-tests:

dist: distdir
	distDir="$(NAME)-$(majorVersion).`svn info | grep Revision | awk ' {print $$2}'`"; \
	$(MV) $(DIST_TEMP) $${distDir}; \
	$(TAR) $(TARFLAGS) $${distDir}.tar $${distDir}; \
	$(RM_RF) $${distDir}; \
	$(COMPRESS) $${distDir}.tar

distclean:

distdir: doc Packages.gz manifest
	$(RM_RF) $(DIST_TEMP)
	$(MKDIR) $(DIST_TEMP)
	for file in `cat MANIFEST`; do \
	  $(MKDIR_P) $(DIST_TEMP)/`dirname $$file`; \
	  $(CP) $$file $(DIST_TEMP)/$$file; \
	done
	$(CP) COPYRIGHT ChangeLog index.html $(DIST_TEMP)/

doc:
	$(RM_RF) $(DOC_DIR)
	$(MAKE) perldoc
	mv $(DOC_DIR)/index.html $(DOC_DIR)/perl.html
	$(MAKE) pydoc
	mv $(DOC_DIR)/index.html $(DOC_DIR)/python.html
	echo "<html>" >> $(DOC_DIR)/index.html
	echo "  <head>  " >> $(DOC_DIR)/index.html
	echo "    <link href="http://inca.sdsc.edu/www/css/style.css" rel="stylesheet" type="text/css" />" >> $(DOC_DIR)/index.html
	echo "    <title>Inca Reporter Repository</title>" >> $(DOC_DIR)/index.html
	echo "  </head>" >> $(DOC_DIR)/index.html
	echo "  <body>" >> $(DOC_DIR)/index.html
	echo "  <center>" >> $(DOC_DIR)/index.html
	echo "    <h2>Inca Reporter Repository</h2>" >> $(DOC_DIR)/index.html
	echo "    <p><a href="../cgi-bin/list_reporters.cgi">View list of reporters</a></p>" >> $(DOC_DIR)/index.html
	echo "    <h2>Inca Reporter API Documentation</h2>" >> $(DOC_DIR)/index.html
	echo "    <p><a href="perl.html">Perl</a></p>" >> $(DOC_DIR)/index.html
	echo "    <p><a href="python.html">Python</a></p>" >> $(DOC_DIR)/index.html
	echo "  </center>" >> $(DOC_DIR)/index.html
	echo "  </body>" >> $(DOC_DIR)/index.html
	echo "</html>" >> $(DOC_DIR)/index.html

install :: install-bin

install-bin:
	if ( test ! -d ${PREFIX}/bin ); then \
    mkdir ${PREFIX}/bin; \
  fi
	for script in ${BIN_FILES}; do \
    $(CP) $${script} $(PREFIX)/bin; \
    $(FIXIN) $(PREFIX)/$${script}; \
    $(CHMOD) $(PERM_RWX) $(PREFIX)/$${script}; \
  done

manifest:
	echo MANIFEST > MANIFEST
	files='Makefile Packages.gz bin cgi-bin etc $(DOC_DIR) lib sbin share version' && \
	while test -n "$${files}"; do \
    subfiles=''; \
	  for file in $${files}; do \
      echo $${file} | grep "\/\.svn\/" >/dev/null; \
      if test $$? -eq 0; then \
        donothingifsvndir=1; \
	    elif test -d $${file}; then \
			  filepattern=`echo $${file} | sed 's/\//\\\\\//g'`; \
				subfilestmp=`ls -A $${file} | sed "s/^/$${filepattern}\//g"`; \
	      subfiles="$${subfiles} $${subfilestmp}"; \
	    else \
	      echo $${file} >> MANIFEST; \
	    fi; \
	  done; \
	  files=$${subfiles}; \
	done

perldoc:
	if ( test ! -d $(DOC_DIR) ); then \
	  mkdir $(DOC_DIR); \
	fi
	for file in $(PERL_FILES); do \
    cp $$file $$file.pod; \
	done
	perl -MPod::Simple::HTMLBatch -e 'Pod::Simple::HTMLBatch::go' "lib/perl:bin:cgi-bin:sbin" $(DOC_DIR)
	for file in bin/* cgi-bin/* sbin/*; do \
    rm -f $$file.pod; \
	done

perltest:
	for file in lib/perl/t/*.t; do \
    perl -I lib/perl $$file; \
  done

pydoc:
	epydoc -o $(DOC_DIR) lib/python/inca

pytest:
	PYTHONPATH=`pwd`/lib/python; export PYTHONPATH; \
	for file in lib/python/t/*.py; do \
    python $$file; \
  done

test: perltest pytest

