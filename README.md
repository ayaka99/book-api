# 書籍管理API

## 概要
Kotlin / Spring Boot / jOOQ / PostgreSQL を用いて実装した書籍管理API。  
書籍と著者の登録・更新、および著者に紐づく書籍の取得機能を提供する。

---

## 設計方針メモ

### 著者と書籍の登録・更新仕様
書籍登録・更新時、著者は以下の2通りの方法で指定可能とする。

- `authorIds`  
  既存著者を指定するためのIDリスト  
  指定されたIDの著者と書籍を紐付ける

- `authors`  
  新規著者を同時作成するための入力情報  
  指定された著者は新規作成し、書籍に紐付ける

更新時は既存の紐付けを一度削除し、  
指定された著者情報で再構築する設計とした。

---

### 同一人物判定について
著者の自動同一人物判定は行わない。

理由：
- 同姓同名の著者が存在し得るため
- `birth_date` がNULL許容のため一意判定が困難
- 別人の著者が誤って統合されることを防ぐため

既存著者を利用する場合は  
`authorIds` を明示的に指定する設計とした。

---

### authorIdの扱い
authorIdは内部識別子だが、APIではリソース識別子として使用する。

UIが存在する場合は、  
著者検索 → 選択 → ID送信  
というフローを想定している。

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

DTOは  
1ファイル1クラス  
の方針で実装している。

---

### トランザクション方針
書籍登録・更新時は

- 書籍本体更新
- 著者作成
- 中間テーブル更新

を1トランザクションで実行し、  
途中失敗時はロールバックされる。

---

### 例外方針
- 業務バリデーション：Service層で実施
- 不正入力：400
- 未存在リソース：404
- DB整合性違反：400

ControllerAdviceでHTTPステータスへ変換している。

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
```bash
docker compose up -d
```
### DB設定

- DB名: mydatabase
- USER: myuser
- PASS: secret
- PORT: 5432

### アプリ起動
```
./gradlew bootRun
```
起動後、以下でAPI利用可能：
```
http://localhost:8080
```
### API例
#### 著者作成
```
POST /authors
Content-Type: application/json
```
```
{
  "name": "著者A",
  "birthDate": "1990-01-01"
}
```
#### 著者更新
```
PUT /authors/1
Content-Type: application/json
```
```
{
  "name": "更新著者",
  "birthDate": "1992-02-02"
}
```
#### 書籍作成
```
POST /books
Content-Type: application/json
```
```
{
  "title": "本A",
  "price": 1000,
  "publicationStatus": 1,
  "authorIds": [1],
  "authors": null
}
```
#### 書籍更新（紐付け置換）
```
PUT /books/2
Content-Type: application/json
```
```
{
  "title": "更新後の本",
  "price": 1500,
  "publicationStatus": 1,
  "authorIds": [1],
  "authors": null
}
```
#### 著者に紐づく書籍取得
```
GET /authors/{authorId}/books
```

---

## テスト

### 実行方法
```
docker compose up -d
./gradlew test
```

### テスト内容
#### AuthorService 単体テスト
- createAuthor_01  
  未来日の生年月日は登録不可（400相当）

- updateAuthor_01  
  更新時も未来日の生年月日は不可

- updateAuthor_02  
  存在しない著者更新は NotFound（404相当）

#### BookService 単体テスト

- createBook_01  
  書籍登録時は最低1人の著者が必須

- updateBookWithAuthors_01  
  既存著者のみ指定時のリレーション置換順序を検証

- updateBookWithAuthors_02  
  新規著者作成を伴う更新処理の順序を検証

- updateBookWithAuthors_03  
  既存著者＋新規著者混在時の紐付け処理を検証

- updateBookWithAuthors_04  
  更新時に著者0人指定は不可

- updateBookWithAuthors_05  
  出版済→未出版の状態遷移は禁止

- updateBookWithAuthors_06  
  価格は0以上のみ許可

#### 結合テスト　例外ハンドリング（SpringBootTest）
- postAuthors_400  
  POST /authors：未来日の生年月日 → 400

- putAuthors_404  
  PUT /authors/{id}：存在しない著者ID → 404

- postBooks_400  
  POST /books：存在しない著者ID指定（整合性エラー） → 400

---

## 要件達成状況

- 書籍と著者の登録
- 書籍と著者の更新
- 著者に紐づく書籍取得