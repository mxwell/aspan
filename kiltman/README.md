# Kiltman

Trie-based KV-store.

## Dependencies

Unpack POCO libraries into a subdirectory under `poco/`.

## Build

```bash
mkdir build
cd build
cmake ../
cmake --build .
```

## Run

Place `forms.csv` with generated verb forms into a working directory.

```
./kiltman
```

Wait for the message `HTTP Server started on port 8080.` and go to `http://localhost:8080/detect?q=абайлар`.