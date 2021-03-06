import os
import sys
import time

ravel_path = "./lib/ravel"


def exe(cmd):
    os.system(cmd)


def build():
    if os.path.exists("myout"):
        exe("rm -rf myout")
    exe("mkdir -p myout")
    exe("find ./src -name *.java | javac -d myout -cp lib/antlr-4.9.1-complete.jar @/dev/stdin")
    print("build finished.")


def ir_gen_executable():
    exe("java -cp ./lib/antlr-4.9.1-complete.jar:./myout PrismCube -i ./bin/test.mx -o ./bin/test.ll -llvm-only")
    exe("scp ./builtin/builtin.ll ./bin/b.ll")
    exe("scp ./bin/test.ll ./bin/t.ll")
    exe("clang ./bin/b.ll ./bin/t.ll -o ./bin/a.out")
    print("ir generate and link finished.")


def run_asm(debug):
    start_time = time.time()
    exe("rm ./bin/*.s")
    exe("rm ./bin/*.ll")
    if debug:
        exe("java -ea -cp ./lib/antlr-4.9.1-complete.jar:./myout PrismCube -i ./bin/test.mx -o ./bin/test.s -emit-asm -emit-llvm ./bin/test.ll -log-o ./bin/log.txt -log-level debug -printV ./bin/virtual.s -O2 -printO2IR ./bin/opt.ll -arch x86_32 -printInnerIR")
    else:
        exe("java -cp ./lib/antlr-4.9.1-complete.jar:./myout PrismCube -i ./bin/test.mx -o ./bin/test.s -emit-asm -arch x86_32")
    exe("scp ./builtin/builtin.s ./bin/b.s")
    exe("scp ./bin/test.s ./bin/t.s")
    print("asm generate finished.")
    gen_end_time = time.time()
    print("generate asm cost time: {}s".format(gen_end_time - start_time))
    if debug:
        exe("{} ./bin/t.s ./bin/b.s --input-file=./bin/std.in".format(ravel_path))
    else:
        exe("{} ./bin/t.s ./bin/b.s".format(ravel_path))
    run_end_time = time.time()
    print("run cost time: {}s".format(run_end_time - gen_end_time))


def run_executable():
    exe("./bin/a.out")


def gen_riscv_asm():
    exe("rm bin/a.ll")
    exe("rm bin/a.s")
    exe("clang -emit-llvm -S bin/a.c -o bin/a.ll --target=riscv32 -O0")
    exe("llc bin/a.ll -o bin/a.s --mattr=+m -march=riscv32")


def compile_test():
    exe("java -cp ./lib/antlr-4.9.1-complete.jar:./myout PrismCube -i ./bin/test.mx -o ./bin/test.ll -emit-llvm")
    exe("java -cp ./lib/antlr-4.9.1-complete.jar:./myout PrismCube -i ./bin/test.mx -o ./bin/test.s -emit-asm")


def parse_test_case(test_case_path):
    with open(test_case_path, 'r') as f:
        lines = f.read().split('\n')
    src_start_idx = lines.index('*/', lines.index('/*')) + 1
    src_text = '\n'.join(lines[src_start_idx:])
    input_start_idx = lines.index('=== input ===') + 1
    input_end_idx = lines.index('=== end ===', input_start_idx)
    input_text = '\n'.join(lines[input_start_idx:input_end_idx])
    output_start_idx = lines.index('=== output ===') + 1
    output_end_idx = lines.index('=== end ===', output_start_idx)
    output_text = '\n'.join(lines[output_start_idx:output_end_idx])
    return src_text, input_text, output_text


def asm_test(dir):
    src_txt, in_txt, out_txt = parse_test_case(dir)
    with open("./bin/test.mx", "w") as f:
        f.write(src_txt)
    with open("./bin/std.in", "w") as f:
        f.write(in_txt)
    with open("./bin/std.out", "w") as f:
        f.write(out_txt)
    exe("java -ea -cp ./lib/antlr-4.9.1-complete.jar:./myout PrismCube -i ./bin/test.mx -o ./bin/test.s -emit-asm -log-o ./bin/log.txt -log-level trace -O3 -print-reg-name -printO2IR ./bin/opt.ll -emit-llvm ./bin/test.ll -arch x86_32 -printInnerIR")
    exe("scp ./builtin/builtin.s ./bin/b.s")
    exe("{} ./bin/test.s ./bin/b.s --input-file=./bin/std.in".format(ravel_path))
    # exe("code ./bin/std.out")


def run():
    i = 1
    case = 0
    conflict = False
    case_dir = "./testcases/codegen/"
    asm_debug = False
    build_flag = True
    while i < len(sys.argv):
        arg = sys.argv[i]
        if arg == "--help" or arg == "-h":
            print("welcome to rainy memory's compiler runner!")
            print("now support:")
            print("---------------------------------------------------------------------")
            print("-h / --help               show help message                          ")
            print("--disable-reload          disable re-build compiler                  ")
            print("-emit-asm                 generate standard rv32i asm for bin/a.c    ")
            print("-compile-test             generate test.ll and test.s for test.mx    ")
            print("ir                        test ir  for bin/test.mx                   ")
            print("asm                       test asm for bin/test.mx                   ")
            print("asm-test <case>           running testcase <case> under codegen      ")
            print("---------------------------------------------------------------------")
            exit(0)
        elif arg == "--disable-reload":
            build_flag = False
        elif arg == "ir":
            if conflict:
                print("error: mode conflict")
                exit(1)
            else:
                conflict = True
            case = 1
        elif arg == "asm":
            if conflict:
                print("error: mode conflict")
                exit(1)
            else:
                conflict = True
            case = 2
            if i + 1 < len(sys.argv) and sys.argv[i + 1] == "debug":
                i = i + 1
                asm_debug = True
        elif arg == "-emit-asm":
            if conflict:
                print("error: mode conflict")
                exit(1)
            else:
                conflict = True
            case = 3
        elif arg == "-compile-test":
            if conflict:
                print("error: mode conflict")
                exit(1)
            else:
                conflict = True
            case = 4
        elif arg == "asm-test":
            if conflict:
                print("error: mode conflict")
                exit(1)
            else:
                conflict = True
            case = 5
            if i + 1 >= len(sys.argv):
                print("error: please specify testcase you wanna run")
                exit(1)
            case_dir = case_dir + sys.argv[i + 1] + ".mx"
            i = i + 1
        else:
            print("error: unknown argument type")
            exit(1)
        i = i + 1
    if build_flag:
        build()
    if case == 1:
        ir_gen_executable()
        run_executable()
    elif case == 2:
        run_asm(asm_debug)
    elif case == 3:
        gen_riscv_asm()
    elif case == 4:
        compile_test()
    elif case == 5:
        asm_test(case_dir)


run()
