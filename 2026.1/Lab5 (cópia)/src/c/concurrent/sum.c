#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>

typedef struct {
    const char *path;
    int sum;
} task_t;

int do_sum(const char *path) {
    FILE *file = fopen(path, "rb");  // open in binary mode
    if (file == NULL) {
        return -1;  // indicate error
    }

    int sum = 0;
    int byte;

    while ((byte = fgetc(file)) != EOF) {
        sum += byte;
    }

    fclose(file);
    return sum;
}

void *worker(void *arg) {
    task_t *t = (task_t *) arg;
    t->sum = do_sum(t->path);
    return NULL;
}

int main(int argc, char *argv[]) {
    int n = argc - 1;
    pthread_t *threads = malloc(n * sizeof(pthread_t));
    task_t *tasks = malloc(n * sizeof(task_t));

    for (int i = 0; i < n; i++) {
        tasks[i].path = argv[i + 1];
        pthread_create(&threads[i], NULL, worker, &tasks[i]);
    }
    for (int i = 0; i < n; i++) {
        pthread_join(threads[i], NULL);
    }
    for (int i = 0; i < n; i++) {
        if (tasks[i].sum >= 0) {
            printf("%s : %d\n", tasks[i].path, tasks[i].sum);
        }
    }

    free(threads);
    free(tasks);
    return 0;
}
