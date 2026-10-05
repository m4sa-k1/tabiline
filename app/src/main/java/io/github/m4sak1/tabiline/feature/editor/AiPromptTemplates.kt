package io.github.m4sak1.tabiline.feature.editor

enum class AiPlanStyle(val title: String, val description: String, val request: String) {
    DESTINATION("行き先が決まっている", "具体的な目的地に合わせて旅程を考える", "目的地：【要入力：行きたい場所・施設・街】\n必ずしたいこと：【任意：希望する体験。なければ「なし」】"),
    REGION("県・地域が決まっている", "その地域のおすすめから目的地を選ぶ", "希望地域：【要入力：都道府県・地域】\n興味：【任意：自然・食・歴史など。なければ「おまかせ」】"),
    REACHABLE("日数から行き先を考える", "出発地から無理なく行ける範囲で提案", "使える日数：【要入力：旅行に使える日数】\n片道の移動時間の上限：【任意：上限。なければ「おまかせ」】\nこの日数で出発地から往復できる範囲で、行き先候補を比較してください。"),
    SURPRISE("完全おまかせ", "条件に合わせて行き先から提案してもらう", "行き先も旅行のテーマもおまかせです。条件に合う候補を理由付きで提案し、私が選んでから旅程を作ってください。\n避けたいこと：【任意：苦手な体験・移動手段。なければ「なし」】"),
}

object AiPromptTemplates {
    fun create(style: AiPlanStyle): String = """あなたは旅行計画を手伝うアシスタントです。以下の条件から、実行可能な旅程を提案してください。
この文章を送る前に【要入力：...】を自分の情報に置き換えます。【任意：...】も希望を書くか、記載された「なし」「おまかせ」などに置き換えます。未記入の項目があれば、JSONを生成する前に私に質問してください。

出発地：【要入力：出発する駅・空港・市区町村。現在地からなら現在地をここに書く】
出発日：【要入力：YYYY-MM-DD形式の年月日】
帰着日・帰着期限：【要入力：YYYY-MM-DDと帰着希望時刻】
予算：【要入力：1人あたりの予算。未定なら「未定」】
人数：【要入力：人数】
交通手段の希望：【任意：電車・飛行機など。なければ「おまかせ」】
${style.request}

重要：位置情報はこのプロンプトからは取得できません。出発地・日付・日数・予算の未記入部分を勝手に推測しないでください。時刻表・運行日・運賃・ターミナル・ゲート・搭乗Group・便番号は公式情報で確認し、出典URLと確認日を旅程説明に記載してください。確認できない情報は事実として作らず、空欄にし、確認が必要なことを説明してください。必要な出発・到着時刻が不明なら、まず質問または候補の確認を行い、実在すると断定したJSONは生成しないでください。予約・購入は行わないでください。

旅程を相談して確定したら、Tabilineの「移動を追加」に読み込めるJSONを作ってください。これはバックアップJSON・GeoJSON・JSON-LD・旅行全体のJSONではありません。
${schemaInstructions}

旅程全体を提案して構いませんが、取り込みは1件ずつです。各移動・空き時間を独立した1つのJSONオブジェクトとして、別々のjsonコードブロックに出してください。配列や複数件を1つのオブジェクトにまとめないでください。利用者は必要なコードブロックを1つずつコピーして取り込みます。1行のJSONでも可。各コードブロック内にはJSON以外の説明を入れないでください。
""".trimIndent()

    val schemaInstructions = """
■ 形式
UTF-8の厳密なJSONオブジェクト、1件あたり最大256KB。コメント・末尾カンマ・省略記号は禁止。ダブルクォートを使い、文字列内の改行・引用符・バックスラッシュをJSONとして正しくエスケープしてください。次の例の日時・地名は形式説明用であり、運行情報ではありません。相談で確定した実際の内容に置き換えてください。
{
  "format": "tabiline.transport-leg",
  "version": 1,
  "mode": "TRAIN",
  "departure": "2026-11-14T09:00:00+09:00",
  "arrival": "2026-11-14T09:30:00+09:00",
  "departureZoneId": "Asia/Tokyo",
  "arrivalZoneId": "Asia/Tokyo",
  "departurePlace": "出発駅",
  "arrivalPlace": "到着駅",
  "trainType": "LOCAL",
  "trainLine": "確認済みの路線名",
  "departureTerminal": "",
  "arrivalTerminal": "",
  "departurePlatform": "",
  "arrivalPlatform": "",
  "boardingGroup": "",
  "flightNumber": "",
  "memo": "",
  "precedingGapType": "WAIT"
}

■ 必須項目
mode、departure、arrival、departureZoneId、arrivalZoneIdは必須。通常の移動はdeparturePlace・arrivalPlaceも空欄不可。
modeはTRAIN（電車）、FLIGHT（飛行機）、FERRY（船）、BUS（バス）、WALK（徒歩）、FREE_TIME（空き時間・用事）、OTHER（その他）のいずれか。
departure・arrivalは年月日と時刻を含むISO 8601形式で、必ず+09:00などのUTCオフセット、またはZを付ける。到着・終了は出発・開始より後。夜行便ではarrivalに翌日の実際の年月日を書く。「翌07:48」などの表示用文字列は禁止。
departureZoneId・arrivalZoneIdはAsia/Tokyoなどの有効なIANAタイムゾーン。日時のオフセットと地域・季節を一致させる。海外・日付変更線をまたぐ場合も実際の瞬間と現地時刻を確認する。

■ 任意項目（文字列は最大20,000文字）
formatを付ける場合はtabiline.transport-leg、versionを付ける場合は整数1。
trainTypeはSHINKANSEN、LIMITED_EXPRESS、EXPRESS、RAPID、LOCAL、OTHERまたはnull。電車以外はnull。電車で省略するとLOCAL。
trainLineは電車の路線名。departurePlatform・arrivalPlatformは乗り場・ホーム・出発ゲート・到着ゲート。
departureTerminal・arrivalTerminalは飛行機の出発・到着ターミナル。boardingGroupは搭乗Group、flightNumberは便番号。飛行機以外ではこれらを空欄にする。
memoはメモ。precedingGapTypeはWAIT（待ち、省略時）またはTRANSFER（ホームなどの移動）。時刻から待ち時間を別のダミー移動として生成しない。
任意の文字列は未指定なら空欄になる。trainType以外のこれらの文字列にnull・数値・配列を使わない。不明な乗り場・ゲートなどは推測せず空文字列にする。

■ 空き時間
modeをFREE_TIMEにし、departurePlaceに用事名を書く（空欄なら「空き時間」）。arrivalPlace・乗り場・交通機関固有項目は空欄。departureは開始、arrivalは終了で、両方のタイムゾーンを同じ地域にする。終了時刻も必須。memoは利用できる。

■ 旅行との紐づけ
通常はtripId・tripNameを省略する（アプリで現在選択中の旅行を維持する）。利用者が明示的に希望し、既存旅行を正確に確認できる場合のみ、tripNameに完全一致の旅行名またはtripIdに既存の整数IDを指定できる。同時指定は禁止。nullは旅行に紐づけない指定。旅行名や内部IDを推測・新規作成しない。
id、sortOrder、trips、legs、createdAtなど、上記にない項目を追加しない。保存する予定のIDと並び順はアプリが管理する。
""".trimIndent()
}
