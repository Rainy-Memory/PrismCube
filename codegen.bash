set ff=UNIX
set -e
scp ./builtin/builtin.s ./builtin.s
cat | java -cp lib/antlr-4.9.1-complete.jar:./myout PrismCube -emit-asm -O3 -o output.s -arch x86_64