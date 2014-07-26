rem   Start a single Go bot
for %%a in (mattpkBOT1) do start "%%a" java -jar kgsGtp.jar %%a.ini & sleep 2
rem   bot started.
