Convert ePub docs to PDF or text

Note: this is a WIP test app. sample book is a free Project Gutenberg book

```
./mvnw clean package

# epub to pdf
 java -jar target/epub-transformer-1.0-SNAPSHOT-jar-with-dependencies.jar book.epub book.pdf

# epub to txt
 java -jar target/epub-transformer-1.0-SNAPSHOT-jar-with-dependencies.jar book.epub book.txt 