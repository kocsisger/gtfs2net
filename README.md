# When using please cite: 
Gergely Kocsis and Imre Varga, gtfs2net: Extraction of General Transit Feed Specification Data Sets to Abstract Networks and Their Analysis,
Published Online: 24 Apr 2023 https://doi.org/10.1089/big.2022.0269

# gtfs2net
Convert GTFS data to a directed graph.

The actual version can be used as a command line tool. Build the project using mvn clean compile assembly:assembly goal.
Then the jar can be run by java -jar nameofjarfile.jar.
This will show the Usage help also.

-----------------------------------------
usage: gtfs2net <br>
-i,--input     gtfs input(s) (required)<br>
-o,--output    gtfs output folder (default: ".")<br>
-r,--radius    node merging radius (default 150)<br>
-s,--step      node merging radius step (default 150)<br>
-t,--type      gtfs input type {zip|dir|dirs} (zip by default, optional)<br>
-v,--verbose   verbose mode
-----------------------------------------

Alternatively you can use the wrapped .exe file:
[gtfs2net.exe](https://github.com/kocsisger/gtfs2net/raw/main/gtfs2net.exe)

