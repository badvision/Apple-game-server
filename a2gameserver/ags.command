#!/bin/sh
#
# AGS - *nix startup shell script
#
# You can set two variables here:
#   1. $MY_JAVA_HOME - to pick a particular java to run under
#   2. $AGS_HOME - to say where you installed ADTPro
#
# Set default AGS_HOME to be the fully qualified
# current working directory.
export AGS_HOME="`dirname \"$0\"`"
cd "$AGS_HOME"
export AGS_HOME=`pwd`

# Uncomment and modify one or both of the lines below if you
# want to specify a particular location for Java or AGS.
# NOTE: be sure to include a trailing slash on MY_JAVA_HOME,
# but not on AGS_HOME.
#
# export MY_JAVA_HOME=/usr/local/java/bin/
# export AGS_HOME=~/myuser/ags

OS=`uname`
OS_ARCH=`uname -p`

# For Linux, use this:
if [ "$OS" = "Linux" ]; then
  if [ "$OS_ARCH" = "i686" ]; then
    export RXTXLIB=lib/rxtx/rxtx-2.2pre2-local/i686-pc-linux-gnu
  else
    if [ "$OS_ARCH" = "i386" ]; then
      export RXTXLIB=lib/rxtx/rxtx-2.2pre2-local/i686-pc-linux-gnu
    else  
      export RXTXLIB=lib/rxtx/rxtx-2.2pre2-local/x86_64-unknown-linux-gnu
    fi
  fi
fi

# For OSX, use this:
if [ "$OS" = "Darwin" ]; then
  if [ "$OS_ARCH" = "powerpc" ]; then
    export RXTXLIB=lib/rxtx/rxtx-2.1-7-bins-r2/Mac_OS_X
  else
    export RXTXLIB=lib/rxtx/rxtx-2.2pre2-local/mac-10.5
  fi
fi

# For Solaris, use this:
if [ "$OS" = "SunOS" ]; then
  export RXTXLIB=lib/rxtx/rxtx-2.2pre2-local/sparc-sun-solaris2.10-32
fi

# Set up the library location.
export TWEAK1="-Djava.library.path="
export TWEAK=$TWEAK1$AGS_HOME/$RXTXLIB

cd "$AGS_HOME"
"$MY_JAVA_HOME"java -Xms256m -Xmx512m "$TWEAK" -cp dist/ags.jar:dist/"$RXTXLIB"/../RXTXcomm.jar: ags.ui.host.Main $*
