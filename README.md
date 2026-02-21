# 書籍管理API

## 概要

Kotlin / Spring Boot / jOOQ / PostgreSQL を用いて実装した書籍管理API。

書籍と著者の登録・更新、および著者に紐づく書籍の取得機能を提供する。

---

## 設計方針メモ

### 著者と書籍の登録・更新仕様

書籍登録・更新時、著者は以下の2通りの方法で指定可能とする。

-   `authorIds`

    既存著者を指定するためのIDリスト

    指定されたIDの著者と書籍を紐付ける

-   `authors` 

    新規著者を同時作成するための入力情報

    指定された著者は新規作成し、書籍に紐付ける

更新時は既存の紐付けを一度削除し、 指定された著者情報で再構築する設計とした。

---

### 同一人物判定について

著者の自動同一人物判定は行わない。

理由:

- 同姓同名の著者が存在し得るため

- `birth_date` がNULL許容のため一意判定が困難

- 別人の著者が誤って統合されることを防ぐため

既存著者を利用する場合は `authorIds` を明示的に指定する設計とした。

---

### authorIdの扱い

authorIdは内部識別子だが、APIではリソース識別子として使用する。

UIが存在する場合は 著者検索 → 選択 → ID送信 というフローを想定している。

ユーザーが直接IDを手入力する想定ではない。

---

### レイヤ構成

- Controller  

  HTTP入出力、DTO変換

- Service  

  業務ロジック、バリデーション、トランザクション管理

- Repository  

  jOOQによるDB操作

---

### DTOを導入した理由

- API契約とドメインモデルの分離
- 将来的な仕様変更耐性
- レスポンス形式の制御

DTOは、1ファイル1クラスの方針で実装している。

---

### トランザクション方針

書籍登録・更新時は

- 書籍本体更新
- 著者作成
- 中間テーブル更新

を1トランザクションで実行し、途中失敗時はロールバックされる。

---

### 例外方針

- 業務バリデーション：Service層で実施
- 不正入力：400
- 未存在リソース：404
- DB整合性違反：400

ControllerAdviceでHTTP変換している。

---

## 実装機能

### 著者

- 作成
- 更新

### 書籍

- 作成
- 更新（著者紐付け置換）

### 取得

- 著者に紐づく書籍一覧取得

---

## 実行方法

### PostgreSQL起動

``` bash
docker compose up -d
```

### DB設定

- DB名: mydatabase
- USER: myuser
- PASS: secret
- PORT: 5432

### ビルド

本プロジェクトはDBが起動していなくてもビルド可能。

``` bash
./gradlew build
```

jOOQ生成コードは `src/main/generated` に同梱している。

スキーマ変更時のみ再生成。

``` bash
./gradlew jooqCodegen
```

### アプリ起動

``` bash
./gradlew bootRun
```

起動後、以下でAPI利用可能：

``` http
http://localhost:8080
```

---

## API例

### 著者作成

``` http
POST /authors
Content-Type: application/json
```

``` json
{
  "name": "著者A",
  "birthDate": "1990-01-01"
}
```

### 著者更新

``` http
PUT /authors/1
Content-Type: application/json
```

``` json
{
  "name": "更新著者",
  "birthDate": "1992-02-02"
}
```

### 書籍作成

``` http
POST /books
Content-Type: application/json
```

``` json
{
  "title": "本A",
  "price": 1000,
  "publicationStatus": 1,
  "authorIds": [1],
  "authors": null
}
```

### 書籍更新

``` http
PUT /books/2
Content-Type: application/json
```

``` json
{
  "title": "更新後の本",
  "price": 1500,
  "publicationStatus": 1,
  "authorIds": [1],
  "authors": null
}
```

### 著者に紐づく書籍取得

``` http
GET /authors/{authorId}/books
```

---

## テスト

### 単体テスト（DB不要）

``` bash
./gradlew test
```

### 結合テスト（DB必要）

``` bash
docker compose up -d
./gradlew integrationTest
```

---

## 要件達成状況

- 書籍と著者の登録
- 書籍と著者の更新
- 著者に紐づく書籍取得