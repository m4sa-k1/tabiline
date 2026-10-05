package io.github.m4sak1.tabiline.feature.editor

enum class AiPlanStyle(val title: String, val description: String, val request: String) {
    DESTINATION("行き先が決まっている", "目的地に合わせて旅全体を計画", "希望の目的地と必ずしたい体験を軸に計画してください。"),
    REGION("県・地域が決まっている", "地域の候補から選ぶ", "希望地域内の候補を比較し、私が選んでから計画してください。"),
    REACHABLE("日数から行き先を考える", "無理なく往復できる旅", "使える日数と移動時間の上限から往復できる候補を比較し、私が選んでから計画してください。"),
    SURPRISE("完全おまかせ", "行き先とテーマから提案", "行き先とテーマを理由付きで複数提案し、私が選んでから計画してください。"),
    DAY_TRIP("日帰り・近場", "その日のうちに帰れる旅", "日帰りできる候補を比較してください。帰着期限を守り、旅行の開始日と終了日は同じ日にしてください。"),
    BUDGET("予算優先", "交通・宿泊・食事も含める", "予算を最優先に候補を比較し、交通・宿泊・食事・入場料の概算内訳と予備費を示してください。"),
    THEME("テーマ・体験重視", "食・自然・歴史などを楽しむ", "指定のテーマと体験を中心に、営業時間と予約の必要性も確認して計画してください。"),
    MULTI_STOP("複数の街を周遊", "行きたい場所を効率よく巡る", "複数の目的地を巡る順序を比較し、滞在時間・乗り換え・移動の負担を考慮して計画してください。"),
    SEISHUN18("青春18きっぷ", "普通・快速列車を中心に旅する", "青春18きっぷを使う旅全体を計画してください。まず公式情報（https://www.jreast.co.jp/tickets/info.aspx?GoodsCd=3001 など）で、旅行する年・季節の発売・利用期間、券の種類、連続する有効日数、1枚につき1人の条件と対象路線・列車・席・特例を確認してください。古い『5回分を分けて使う・複数人で共有』という条件を使わないでください。JRの普通・快速列車の普通車自由席を基本とし、新幹線・特急・私鉄・一般のバスなどをきっぷだけで利用できると扱わないでください。BRT・宮島フェリー・通過特例・北海道新幹線オプション券などは適用条件を公式情報で確認し、対象外の区間・別料金・訪問税は旅程とmemoに明記してください。利用期間外・券の期限外なら勝手に適用せず、日程変更や別料金の案を私に確認してください。長時間の乗車負担と乗り換えに余裕を持たせ、旅行先でしたいことの時間も確保してください。"),
}

enum class AiPromptField(val label: String, val hint: String) {
    FROM("出発地", "出発する駅・空港・市区町村。現在地はここに記入"),
    START("出発日・時刻", "YYYY-MM-DDと出発可能な時刻"),
    RETURN("帰着日・期限", "YYYY-MM-DDと帰着希望時刻"),
    BUDGET("予算", "1人あたりの総予算。未定なら「未定」"),
    PEOPLE("人数", "人数と、必要なら子どもの年齢"),
    DESTINATION("目的地", "行きたい場所・施設・街。周遊なら複数記入"),
    REGION("希望地域", "都道府県・地域"),
    DAYS("使える日数", "旅行に使える日数"),
    THEME("テーマ・興味", "食・自然・歴史・鉄道など"),
    MUST("旅行先でしたいこと", "希望する体験・観光・食事・訪問先。未定なら「おまかせ」"),
    PASS("青春18きっぷの種類・利用開始日", "3日間用・5日間用など。旅行する年の公式条件を確認。未定なら「未定」"),
    AVOID("避けたいこと", "苦手な体験・移動手段・混雑など"),
    MAX_TRAVEL("片道の移動時間上限", "例：3時間以内"),
    LODGING("宿泊の希望", "宿泊地域・宿の種類・予算。日帰りなら不要"),
    FIXED("確定済みの予定", "予約済みの便・宿・イベントと正確な日時"),
    NEEDS("配慮してほしいこと", "歩行距離・段差・食事など。個人情報は必要最小限に"),
    BUFFER("乗り換えの余裕", "例：同じ駅15分、空港は公式の締切を厳守"),
}

data class AiPromptOptions(val values: Map<AiPromptField, String> = emptyMap(), val pace: String = "標準", val transport: Set<String> = emptySet())

object AiPromptTemplates {
    fun requiredFields(style: AiPlanStyle): Set<AiPromptField> = setOf(
        AiPromptField.FROM, AiPromptField.START, AiPromptField.RETURN, AiPromptField.BUDGET, AiPromptField.PEOPLE, AiPromptField.MUST,
    ) + when (style) {
        AiPlanStyle.DESTINATION, AiPlanStyle.MULTI_STOP -> setOf(AiPromptField.DESTINATION)
        AiPlanStyle.REGION -> setOf(AiPromptField.REGION)
        AiPlanStyle.REACHABLE -> setOf(AiPromptField.DAYS)
        AiPlanStyle.THEME -> setOf(AiPromptField.THEME)
        AiPlanStyle.SEISHUN18 -> setOf(AiPromptField.PASS)
        else -> emptySet()
    }

    fun create(style: AiPlanStyle, options: AiPromptOptions = AiPromptOptions()): String {
        val required = requiredFields(style)
        val conditions = AiPromptField.entries.joinToString("\n") { field ->
            "${field.label}：" + options.values[field]?.trim().orEmpty().take(2000).ifBlank {
                if (field in required) "【要入力：${field.hint}】" else "【任意：${field.hint}。なければ「おまかせ」】"
            }
        }
        return """あなたは旅行計画を手伝うアシスタントです。移動1件ではなく、出発から帰着までの旅全体を一緒に考えてください。
未記入の【要入力：…】は私の情報に置き換えます。【任意：…】は希望か「なし」「おまかせ」に置き換えます。未記入・矛盾・曖昧な条件があれば、JSONを生成する前に私に質問してください。位置情報は自動取得できません。

計画の種類：${style.title}
${style.request}
$conditions
旅のペース：${options.pace}
希望する交通手段：${options.transport.sorted().joinToString("・").ifBlank { "おまかせ" }}

■ 計画の進め方
出発地・日付・日数・予算を勝手に推測しないでください。確定済みの予定を優先し、歩行・乗り換え・食事・休憩・観光・宿泊に余裕を持たせてください。観光・用事・休憩はFREE_TIME、徒歩移動はWALKで表せます。同じ場所での単なる待ちはダミー予定にせず、次の予定のprecedingGapTypeで表してください。宿泊も必要に応じてFREE_TIMEとして含めてください。
時刻表・運行日・運賃・営業時間・ターミナル・ゲート・搭乗Group・便番号は公式情報で確認し、出典URLと確認日をJSON外の旅程説明に示してください。確認できない情報を事実として作らないでください。不明な任意項目は空文字列にし、確認が必要と明記してください。必須の出発・到着時刻が不明なら、まず質問・確認を行い、実在すると断定したJSONを生成しないでください。予約・購入は行わないでください。
まず候補と日ごとの旅程・概算費用・注意点を提案し、私の確認後に次の厳密な形式の旅行JSONを1つだけ出力してください。

$schemaInstructions
""".trimIndent()
    }

    val schemaInstructions = """
■ Tabilineの旅行追加用JSON
バックアップJSON・GeoJSON・JSON-LD・移動1件用JSONではありません。UTF-8、最大1MBの単一オブジェクトに旅行1件と予定1〜200件をlegs配列で含めます。各予定は最大256KB。1行でも可。ダブルクォートを使い、文字列を正しくエスケープし、コメント・末尾カンマ・省略記号を入れないでください。最終出力は1つのjsonコードブロックにJSON全体をまとめ、ブロック内には説明を入れないでください。
以下は形式説明用であり運行情報ではありません。相談で確定した実際の内容に置き換えてください。
{
  "format": "tabiline.trip",
  "version": 1,
  "trip": {"name": "旅行名", "startDate": "2026-11-14", "endDate": "2026-11-14", "note": "テーマ・注意点"},
  "legs": [
    {
      "mode": "TRAIN",
      "departure": "2026-11-14T09:00:00+09:00",
      "arrival": "2026-11-14T09:30:00+09:00",
      "departureZoneId": "Asia/Tokyo",
      "arrivalZoneId": "Asia/Tokyo",
      "departurePlace": "出発駅", "arrivalPlace": "到着駅",
      "trainType": "LOCAL", "trainLine": "確認済みの路線名",
      "departureTerminal": "", "arrivalTerminal": "",
      "departurePlatform": "", "arrivalPlatform": "",
      "boardingGroup": "", "flightNumber": "", "memo": "", "precedingGapType": "WAIT"
    }
  ]
}

■ 旅行の項目
formatは必ずtabiline.trip、versionは整数1。tripはオブジェクトでname・startDate・endDateが必須、noteは任意の文字列（省略時は空欄）。nameは空欄不可。日付はYYYY-MM-DD。終了日は開始日以降、旅行期間は最大366日。全予定の現地出発日と現地到着日をこの期間内に含めてください。必ず新しい旅行として追加されます。

■ legs配列の予定
mode・departure・arrival・departureZoneId・arrivalZoneIdは必須。通常の移動はdeparturePlace・arrivalPlaceも空欄不可。
modeはTRAIN（電車）・FLIGHT（飛行機）・FERRY（船）・BUS（バス）・WALK（徒歩）・FREE_TIME（空き時間・用事）・OTHERのいずれか。
departure・arrivalは年月日と時刻を含むISO 8601形式で+09:00などのUTCオフセット、またはZを必ず付ける。到着・終了は出発・開始より後。夜行便のarrivalは翌日の実際の年月日を使い、「翌07:48」などの表示用文字列にしない。タイムゾーンはAsia/Tokyoなどの有効なIANA名。地域・季節・海外の日付変更も考慮し、オフセットと現地時刻を一致させる。出発順に並べ、重複する時刻は相談して解消してください。
trainTypeはSHINKANSEN・LIMITED_EXPRESS・EXPRESS・RAPID・LOCAL・OTHERまたはnull。電車で省略時はLOCAL、電車以外はnull。trainLineは路線名。
departurePlatform・arrivalPlatformは出発と到着の乗り場・ホーム・ゲート。departureTerminal・arrivalTerminalは出発と到着の空港ターミナル。boardingGroupは搭乗Group、flightNumberは便番号。飛行機以外では空港固有項目を空欄にする。
memoはメモ。precedingGapTypeはWAIT（同じホームなどで待つ、省略時）またはTRANSFER（ホームなどを移動する）。不明な乗り場・ゲート・便番号を推測しない。
任意の文字列は省略時に空欄。文字列は最大20,000文字。trainType以外の文字列にnull・数値・配列を使わない。
FREE_TIMEではdeparturePlaceに用事名（空欄なら「空き時間」）、arrivalPlace・乗り場・交通機関固有項目は空欄。departureは開始、arrivalは終了で終了時刻も必須。両方のタイムゾーンは同じ地域にする。memoは利用可能。
id・tripId・tripName・sortOrder・createdAtなど、上記にない項目は追加しない。内部ID・紐づけ・並び順はアプリが管理します。設定や既存データを含めないでください。
""".trimIndent()
}
