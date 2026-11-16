import sys
import threading

def do_sum(path):
    _sum = 0
    with open(path, 'rb',buffering=0) as f:
        byte = f.read(1)
        while byte:
            _sum += int.from_bytes(byte, byteorder='big', signed=False)
            byte = f.read(1)
        return _sum

def worker(path, results, i):
    results[i] = do_sum(path)

if __name__ == "__main__":
    paths = sys.argv[1:]
    results = [0] * len(paths)
    threads = [threading.Thread(target=worker, args=(p, results, i)) for i, p in enumerate(paths)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    for path, s in zip(paths, results):
        print(path + " : " + str(s))
