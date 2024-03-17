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

- place `forms.csv` with generated verb forms into a working directory
- convert: read `forms.csv`, build a trie and dump it serialized to `trie.txt`:

  ```
  ./kiltman convert trie.txt
  ```
- load the trie and serve via HTTP
  ```
  ./kiltman load trie.txt
  ```
- wait for the message `HTTP Server started on port 8080.` and go to `http://localhost:8080/detect?q=абайлар`.