# 移動追加JSON / AI interchange contract

AI機能が生成する1件の移動・空き時間を入力フォームへ取り込むための仕様です。ユーザーに手作業でJSONを作成させることは想定していません。バックアップのJSONとは別形式です。

読み込みは登録を実行せず、入力フォームを置き換えます。利用者が確認して「保存」を押したときだけ新しい予定として登録します。現在は1ファイルにつき1件です。複数件の配列・既存予定の更新・旅行の自動作成は行いません。

## 形式

UTF-8、最大256KB。「JSONを読み込む」は端末のファイル選択、「JSONをコピペで読み込む」は貼り付け専用です。アプリは外部サーバーへアップロードしません。JSONオブジェクトは1行でも整形済みでも受け付け、文字列のエスケープとUnicodeを保持します。UTF-8 BOMにも対応します。

AI回答の全体が ` ```json ` または ` ``` ` で囲まれている場合も受け付けます（1行の囲みも可）。JSON以外の説明文を混ぜないでください。コメント・末尾カンマ・重複した項目名・シングルクォート・複数オブジェクトの連結は拒否します。

## AIプロンプト

アプリ内で「行き先が決まっている」「県・地域が決まっている」「日数から行き先を考える」「完全おまかせ」の4種類を選んでコピーできます。プロンプト内にこの仕様の全項目・具体例・確認事項を含むため、AIがリンクを閲覧できなくても読み込み形式を確認できます。

コピー後は `【要入力：…】` と `【任意：…】` を自分の情報に置き換えてAIへ送ります。現在地を自動取得せず、AIへ自動送信もしません。時刻表などは公式情報で確認するよう指示し、未記入条件や未確認の日時を勝手に補完しないよう求めます。複数の予定は1件ごとに独立したJSONコードブロックで出力させ、各ブロックを個別に読み込みます。

```json
{
  "format": "tabiline.transport-leg",
  "version": 1,
  "tripName": "東京旅行",
  "mode": "FLIGHT",
  "departure": "2026-11-14T09:00:00+09:00",
  "arrival": "2026-11-14T10:15:00+09:00",
  "departureZoneId": "Asia/Tokyo",
  "arrivalZoneId": "Asia/Tokyo",
  "departurePlace": "大阪国際空港",
  "arrivalPlace": "羽田空港",
  "trainType": null,
  "trainLine": "",
  "departureTerminal": "南ターミナル",
  "arrivalTerminal": "第2ターミナル",
  "departurePlatform": "10",
  "arrivalPlatform": "58",
  "boardingGroup": "2",
  "flightNumber": "NH20",
  "memo": "実際の時刻と搭乗情報は航空会社で確認してください。",
  "precedingGapType": "WAIT"
}
```

この例は入力形式の説明用です。実在の運航時刻や搭乗情報を保証しません。

## 項目

| 項目 | 内容 |
| --- | --- |
| `format`, `version` | 任意。指定する場合は上記の値のみ |
| `mode` | 必須：`TRAIN`, `FLIGHT`, `FERRY`, `BUS`, `WALK`, `FREE_TIME`, `OTHER` |
| `departure`, `arrival` | 必須。UTCの`Z`または明示的なオフセットを含むISO 8601日時。到着・終了は出発・開始より後。日またぎにも対応 |
| `departureZoneId`, `arrivalZoneId` | 必須。IANAタイムゾーン。空き時間は両方とも同じ地域 |
| `departurePlace`, `arrivalPlace` | 移動の場合は空欄不可。空き時間は`departurePlace`が用事名（未指定・空欄は空き時間）、`arrivalPlace`は空欄 |
| `tripId` または `tripName` | 任意、同時指定不可。既存の手動旅行へ紐づける。名前指定は完全一致で1件に特定できる場合のみ。`null`は紐づけなし。省略時は入力フォームで選択中の旅行を維持 |
| `trainType` | 任意、`null`可。`SHINKANSEN`, `LIMITED_EXPRESS`, `EXPRESS`, `RAPID`, `LOCAL`, `OTHER`。電車で省略した場合は`LOCAL` |
| `trainLine` | 路線名 |
| `departureTerminal`, `arrivalTerminal` | 出発・到着ターミナル |
| `departurePlatform`, `arrivalPlatform` | 出発・到着の乗り場／ゲート |
| `boardingGroup`, `flightNumber` | 搭乗Group・便番号 |
| `memo` | メモ。空き時間にも対応 |
| `precedingGapType` | `WAIT`（省略時）または`TRANSFER` |

任意の文字列を省略した場合は空欄になります（`null`とは異なります）。各文字列は最大20,000文字です。路線・種別は電車、ターミナル・Group・便番号は飛行機で保存されます。空き時間に到着地・乗り場を指定しないでください。

`id`・`sortOrder`などの内部項目は指定できません。登録後のID・並び順・紐づけなしの日付名の旅行はアプリが管理します。未知の項目・不正な型・不明な旅行・無効な日時は拒否し、元の入力内容や登録済みデータを変更しません。将来のAI連携では、実際の旅行一覧を文脈として渡し、この仕様に一致するJSONを生成させます。
