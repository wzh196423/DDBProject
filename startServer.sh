#!/usr/bin/env bash

# make lock manager
cd src/lockmgr
make clean
make


# make server
cd ../transaction
make clean
make server
make client

# run
make runregistry &
make runtm &
make runrmflights &
make runrmrooms &
make runrmcars &
make runrmcustomers &
make runwc &