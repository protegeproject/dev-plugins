#!/bin/sh

host=$1
port=$2
project=$3
timeout1=$4
timeout2=$5

cp=classes:${PROTEGE_HOME}/protege.jar
for file in ${PROTEGE_HOME}/plugins/edu.stanford.smi.protegex.owl/*.jar
do
  cp=${cp}:${file}
done

java -cp ${cp} \
     -Djava.util.logging.config.file=config/logging.properties \
     -Xmx700M \
     -Dprotege.dir=${PROTEGE_HOME} \
     simulator.Simulator ${host} ${port} ${project} ${timeout1} ${timeout2}