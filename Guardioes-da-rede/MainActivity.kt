package br.cefetmg.guardioes

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

private val NAVY = Color.parseColor("#0F2238")
private val PANEL = Color.parseColor("#172F49")
private val PANEL2 = Color.parseColor("#2B4C73")
private val LINE = Color.parseColor("#3C5878")
private val GOLD = Color.parseColor("#F6C85F")
private val CREAM = Color.parseColor("#F5F1E7")
private val MUTED = Color.parseColor("#A9B8C9")

private val imagensCasosNovos = mapOf(
    "perfil_imitador" to "identidade_e_perfis",
    "permissoes_demais" to "links_apps_e_arquivos",
    "qr_premiado" to "links_apps_e_arquivos",
    "localizacao_story" to "privacidade_e_dados",
    "codigo_confirmacao" to "privacidade_e_dados",
    "arquivo_misterioso" to "links_apps_e_arquivos",
    "amizade_perfil_falso" to "identidade_e_perfis",
    "noticia_assustou" to "videos_e_informacao",
    "audio_fora_contexto" to "videos_e_informacao",
    "senha_repetida" to "privacidade_e_dados",
    "compra_surpresa" to "tv_compartilhada",
    "quiz_dados" to "privacidade_e_dados",
    "conta_esquecida_tv" to "tv_compartilhada",
    "app_gratis" to "tv_compartilhada",
    "video_classificacao" to "videos_e_informacao",
    "pagina_inadequada" to "videos_e_informacao",
    "bloqueio_etario" to "tv_compartilhada"
)

private fun JSONArray.objs(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
private fun JSONArray.strs(): List<String> = (0 until length()).map { getString(it) }

/** Estado de um caso. Só ids e inteiros: ocupa poucos bytes. */
class CaseState {
    val collected = linkedSetOf<String>()
    val analyzed = linkedSetOf<String>()
    val notes = mutableMapOf<String, Int>()
    val notices = mutableMapOf<String, String>()
    val task = mutableMapOf<String, String>()
    var quem = ""
    var arma = ""
    var onde = ""
    var choice = ""
    val closed: Boolean get() = choice.isNotEmpty()

    fun toJson(): JSONObject = JSONObject().apply {
        put("c", JSONArray(collected.toList()))
        put("a", JSONArray(analyzed.toList()))
        put("n", JSONObject(notes.toMap()))
        put("m", JSONObject(notices.toMap()))
        put("t", JSONObject(task.toMap()))
        put("q", quem); put("w", arma); put("o", onde); put("d", choice)
    }

    companion object {
        fun from(j: JSONObject): CaseState {
            val s = CaseState()
            j.optJSONArray("c")?.strs()?.let { s.collected.addAll(it) }
            j.optJSONArray("a")?.strs()?.let { s.analyzed.addAll(it) }
            j.optJSONObject("n")?.let { o -> o.keys().forEach { k -> s.notes[k] = o.getInt(k) } }
            j.optJSONObject("m")?.let { o -> o.keys().forEach { k -> s.notices[k] = o.getString(k) } }
            j.optJSONObject("t")?.let { o -> o.keys().forEach { k -> s.task[k] = o.getString(k) } }
            s.quem = j.optString("q"); s.arma = j.optString("w")
            s.onde = j.optString("o"); s.choice = j.optString("d")
            return s
        }
    }
}

class MainActivity : Activity() {
    private lateinit var ambientes: List<JSONObject>
    private lateinit var casos: List<JSONObject>
    private lateinit var prefs: SharedPreferences
    private val states = mutableMapOf<String, CaseState>()
    private var cur: JSONObject? = null
    private var back: (() -> Unit)? = null
    private var sv: ScrollView? = null
    private var lastKey = ""
    private var avisoId = ""
    private var avisoTxt = ""

    private val caso: JSONObject get() = cur!!
    private val st: CaseState get() = states[caso.getString("id")]!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val json = assets.open("casos.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        ambientes = root.getJSONArray("ambientes").objs()
        casos = root.getJSONArray("casos").objs()
        prefs = getSharedPreferences("guarda", MODE_PRIVATE)
        val saved = try { JSONObject(prefs.getString("estado", "{}") ?: "{}") } catch (e: Exception) { JSONObject() }
        casos.forEach { c ->
            val id = c.getString("id")
            states[id] = try { saved.optJSONObject(id)?.let { CaseState.from(it) } ?: CaseState() } catch (e: Exception) { CaseState() }
        }
        home()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Usado para voltar entre telas")
    override fun onBackPressed() {
        val b = back
        if (b != null) b() else super.onBackPressed()
    }

    // ---------- util de interface ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun box(fill: Int, stroke: Int, w: Int) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(12).toFloat(); setStroke(dp(w), stroke)
    }

    private fun label(t: String, sp: Float = 18f, color: Int = CREAM, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = sp; setTextColor(color); setLineSpacing(0f, 1.15f)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun card(t: String, color: Int = CREAM) = label(t, 17f, color).apply {
        setPadding(dp(16), dp(12), dp(16), dp(12)); background = box(PANEL, LINE, 1)
    }

    private fun btn(t: String, primary: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) = Button(this).apply {
        text = t; isAllCaps = false; textSize = 18f
        setTextColor(if (primary) NAVY else CREAM)
        minHeight = dp(56); minimumHeight = dp(56)
        stateListAnimator = null
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        setPadding(dp(20), dp(10), dp(20), dp(10))
        val normal = if (primary) box(GOLD, GOLD, 2) else box(PANEL, LINE, 2)
        val focus = if (primary) box(GOLD, CREAM, 5) else box(PANEL2, GOLD, 5)
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), focus)
            addState(intArrayOf(android.R.attr.state_pressed), focus)
            addState(intArrayOf(), normal)
        }
        isEnabled = enabled
        alpha = if (enabled) 1f else 0.45f
        setOnClickListener { onClick() }
    }

    private fun LinearLayout.figuras(altura: Int, vararg itens: Pair<String, String>) {
        val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        itens.forEach { (nomeImg, desc) ->
            val rid = resources.getIdentifier(nomeImg, "drawable", packageName)
            if (rid != 0) {
                val iv = ImageView(this@MainActivity).apply {
                    setImageResource(rid); scaleType = ImageView.ScaleType.FIT_CENTER; contentDescription = desc
                }
                row.addView(iv, LinearLayout.LayoutParams(0, dp(altura), 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
            }
        }
        if (row.childCount > 0) add(row, 6)
    }

    private fun LinearLayout.imagensCaso(id: String) {
        when (id) {
            "moedas_sumidas" -> figuras(300, "mascote_permissoes" to "Agente da Guarda Digital mostrando um app que pede acesso a contatos e mensagens")
            "foto_vazada" -> figuras(280,
                "mascote_repost" to "Agente em cadeira de rodas mostrando uma foto repostada sem autorização",
                "mascote_localizacao" to "Agente em cadeira de rodas mostrando a localização ativada em uma publicação")
            "grupo_turma" -> figuras(400,
                "chat_igualdade" to "Chat da escola fictício com comentários ofensivos",
                "chat_brincadeira" to "Chat da escola fictício em que a ofensa é chamada de brincadeira",
                "chat_apoio" to "Chat da escola fictício com uma colega oferecendo apoio")
        }

        val nomeDrawable = imagensCasosNovos[id] ?: return
        val rid = resources.getIdentifier(nomeDrawable, "drawable", packageName)
        if (rid != 0) {
            val iv = ImageView(this@MainActivity).apply {
                setImageResource(rid)
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                contentDescription = "Ilustração temática principal do caso"
            }
            addView(iv, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(180)
            ).apply {
                topMargin = dp(2)
                bottomMargin = dp(2)
            })
        }
    }

    private fun LinearLayout.add(v: View, top: Int = 10) {
        addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) })
    }

    private fun screen(key: String, backAction: (() -> Unit)?, build: LinearLayout.() -> Unit) {
        back = backAction
        val k = key + "/" + (cur?.optString("id") ?: "")
        val keep = if (k == lastKey) (sv?.scrollY ?: 0) else 0
        lastKey = k
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(24), dp(32), dp(40))
        }
        col.build()
        val s = ScrollView(this).apply { setBackgroundColor(NAVY); addView(col) }
        sv = s
        setContentView(s)
        if (keep > 0) s.post { s.scrollTo(0, keep) }
    }

    private fun save() {
        try {
            val o = JSONObject()
            states.forEach { (k, v) -> o.put(k, v.toJson()) }
            prefs.edit().putString("estado", o.toString()).apply()
        } catch (e: Exception) { /* sem progresso salvo, o jogo continua */ }
    }

    private fun nome(items: List<JSONObject>, id: String) =
        items.firstOrNull { it.getString("id") == id }?.getString("nome") ?: "—"

    private fun tarefaOk(c: JSONObject, s: CaseState) =
        c.getJSONObject("tarefa").getJSONArray("itens").objs().all { it.getString("id") in s.task }

    private fun score(): Pair<Int, Int> {
        var seg = 0; var emp = 0
        casos.forEach { c ->
            val x = states[c.getString("id")]!!
            if (!x.closed) return@forEach
            val sol = c.getJSONObject("solucao")
            if (x.quem == sol.getString("quem") && x.arma == sol.getString("arma") && x.onde == sol.getString("onde")) seg += 2
            c.getJSONObject("reuniao").getJSONArray("opcoes").objs()
                .firstOrNull { it.getString("id") == x.choice }?.getJSONObject("impacto")
                ?.let { seg += it.getInt("seguranca"); emp += it.getInt("empatia") }
            c.getJSONArray("notificacoes").objs().forEach { n ->
                n.getJSONArray("acoes").objs().firstOrNull { it.getString("id") == x.notices[n.getString("id")] }
                    ?.getJSONObject("impacto")?.let { seg += it.getInt("seguranca"); emp += it.getInt("empatia") }
            }
        }
        return seg to emp
    }

    // ---------- telas ----------
    private fun home() {
        cur = null
        screen("home", null) {
            add(label("AGÊNCIA GUARDA DIGITAL", 14f, GOLD, true), 0)
            add(label("Guardiões da Rede: Casos Digitais", 30f, CREAM, true), 4)
            figuras(200, "mascote_acolhedora" to "Agente da Guarda Digital apresentando o jogo")
            add(label("Escolha um caso. Todas as pessoas e situações são fictícias. Observe, pense, cuide.", 18f, MUTED))
            val (seg, emp) = score()
            add(card("Pontos de Segurança: $seg    Pontos de Empatia: $emp", GOLD))

            val categorias = listOf(
                Triple("Proteção de Dispositivos", listOf("moedas_sumidas", "qr_premiado", "conta_esquecida_tv", "app_gratis", "bloqueio_etario"), 18),
                Triple("Proteção de Dados", listOf("perfil_imitador", "permissoes_demais", "localizacao_story", "codigo_confirmacao", "arquivo_misterioso", "amizade_perfil_falso", "senha_repetida", "quiz_dados"), 24),
                Triple("Promoção do Bem-Estar", listOf("foto_vazada", "grupo_turma", "noticia_assustou", "audio_fora_contexto", "compra_surpresa", "video_classificacao", "pagina_inadequada"), 24)
            )

            categorias.forEach { (nomeCategoria, idsCategoria, topSpacing) ->
                add(label(nomeCategoria, 22f, GOLD, true), topSpacing)

                val casosDaCategoria = casos.filter { c -> c.getString("id") in idsCategoria }

                casosDaCategoria.forEachIndexed { index, c ->
                    val num = index + 1
                    val done = states[c.getString("id")]!!.closed
                    val seloStr = if (done) " (Selo: " + c.getString("selo") + ")" else ""
                    val t = "$num. " + c.getString("titulo") + seloStr

                    add(btn(t) {
                        cur = c
                        if (states[c.getString("id")]!!.closed || states[c.getString("id")]!!.collected.isNotEmpty()) hub() else chamado()
                    }, 8)
                }
            }
        }
    }

    private fun chamado() {
        val c = caso
        screen("chamado", { home() }) {
            add(label(c.getString("titulo"), 28f, CREAM, true), 0)
            add(label(c.getString("competencia"), 16f, GOLD))
            add(card(c.getString("chamado")))
            add(card(c.getString("dialogo")))
            imagensCaso(c.getString("id"))
            add(card("Objetivo: " + c.getString("objetivo")))
            add(card(c.getString("aviso_seguro"), GOLD))
            add(btn("Explorar o mapa", primary = true) { hub() }, 20)
            add(btn("Voltar aos casos") { home() })
        }
    }

    private fun hub() {
        val c = caso; val s = st
        val pistas = c.getJSONArray("pistas").objs()
        val nots = c.getJSONArray("notificacoes").length()
        val periciado = pistas.all { it.getString("id") in s.analyzed }
        val tOk = tarefaOk(c, s)
        screen("hub", { home() }) {
            add(label(c.getString("titulo"), 28f, CREAM, true), 0)
            add(label(c.getString("competencia"), 16f, GOLD))
            add(card("Pistas coletadas: ${s.collected.size}/${pistas.size}\nPerícias feitas: ${s.analyzed.size}/${pistas.size}\n" +
                    "Alertas respondidos: ${s.notices.size}/$nots\nMissão rápida: ${if (tOk) "concluída" else "pendente"}"))
            add(btn("Mapa dos ambientes") { mapa() })
            add(btn("Bancada de perícia", enabled = s.collected.isNotEmpty()) { bancada() })
            add(btn("Alertas") { alertas() })
            add(btn("Missão rápida", enabled = periciado) { tarefa() })
            add(btn("Caderno de hipóteses") { caderno() })
            val pronto = periciado && tOk && s.notices.size == nots
            add(btn(if (s.closed) "Ver desfecho" else "Reunião de emergência", primary = true, enabled = s.closed || pronto) { reuniao() }, 20)
            if (!pronto && !s.closed) add(label("A reunião abre depois de coletar e periciar as pistas, responder os alertas e fazer a missão rápida.", 15f, MUTED))
            add(btn("Voltar aos casos") { home() })
        }
    }

    private fun mapa() {
        val c = caso; val s = st
        screen("mapa", { hub() }) {
            add(label("Mapa dos ambientes", 28f, CREAM, true), 0)
            add(label("Procure uma pista em cada ambiente.", 17f, MUTED))
            ambientes.forEach { a ->
                add(label(a.getString("nome"), 22f, GOLD, true), 22)
                add(label(a.getString("descricao"), 15f, MUTED), 2)
                c.getJSONArray("pistas").objs().filter { it.getString("ambiente_id") == a.getString("id") }.forEach { p ->
                    val pid = p.getString("id")
                    if (pid in s.collected) {
                        add(card("✔ " + p.getString("titulo") + "\n" + p.getString("descricao")))
                    } else {
                        add(btn("Coletar pista neste ambiente") { s.collected.add(pid); save(); mapa() })
                    }
                }
            }
            add(btn("Voltar ao caso") { hub() }, 24)
        }
    }

    private fun bancada() {
        val c = caso; val s = st
        screen("bancada", { hub() }) {
            add(label("Bancada de perícia", 28f, CREAM, true), 0)
            add(label("Cada perícia só explica uma pista pronta do caso. Nada aqui consulta contas reais.", 16f, MUTED))
            c.getJSONArray("pistas").objs().filter { it.getString("id") in s.collected }.forEach { p ->
                val per = p.getJSONObject("pericia")
                add(label(p.getString("titulo"), 20f, GOLD, true), 22)
                if (p.getString("id") in s.analyzed) {
                    add(card(per.getString("titulo") + "\n\nEntrada: " + per.getString("entrada") +
                            "\n\nResultado: " + per.getString("resultado") + "\n\n" + per.getString("explicacao")))
                } else {
                    add(btn("Fazer perícia: " + per.getString("titulo")) { s.analyzed.add(p.getString("id")); save(); bancada() })
                }
            }
            add(btn("Voltar ao caso") { hub() }, 24)
        }
    }

    private fun alertas() {
        val c = caso; val s = st
        screen("alertas", { hub() }) {
            add(label("Alertas", 28f, CREAM, true), 0)
            add(label("Decida como agir. Vamos pensar juntos: cada escolha tem um retorno.", 16f, MUTED))
            c.getJSONArray("notificacoes").objs().forEach { n ->
                val nid = n.getString("id")
                add(label(n.getString("titulo"), 20f, GOLD, true), 22)
                add(card(n.getString("mensagem")))
                val acoes = n.getJSONArray("acoes").objs()
                val ans = s.notices[nid]
                if (ans == null) {
                    acoes.forEach { a -> add(btn(a.getString("rotulo")) { s.notices[nid] = a.getString("id"); save(); alertas() }) }
                } else {
                    val a = acoes.first { it.getString("id") == ans }
                    add(card("Você escolheu: " + a.getString("rotulo") + "\n\n" + a.getString("feedback"), GOLD))
                }
            }
            add(btn("Voltar ao caso") { hub() }, 24)
        }
    }

    private fun tarefa() {
        val c = caso; val s = st
        val t = c.getJSONObject("tarefa")
        val aId = avisoId; val aTxt = avisoTxt
        avisoId = ""; avisoTxt = ""
        screen("tarefa", { hub() }) {
            add(label(t.getString("titulo"), 28f, CREAM, true), 0)
            add(card(t.getString("instrucao")))
            t.getJSONArray("itens").objs().forEach { item ->
                val iid = item.getString("id")
                val ans = s.task[iid]
                add(label(item.getString("pergunta"), 20f, GOLD, true), 22)
                item.getJSONArray("opcoes").objs().forEach { o ->
                    val oid = o.getString("id")
                    add(btn((if (ans == oid) "✔ " else "") + o.getString("texto"), enabled = ans == null) {
                        if (o.getBoolean("correta")) {
                            s.task[iid] = oid; save()
                        } else {
                            avisoId = iid; avisoTxt = o.getString("feedback")
                        }
                        tarefa()
                    })
                }
                if (ans != null) {
                    val o = item.getJSONArray("opcoes").objs().first { it.getString("id") == ans }
                    add(card(o.getString("feedback"), GOLD))
                } else if (aId == iid && aTxt.isNotEmpty()) {
                    add(card("Vamos pensar juntos: " + aTxt, GOLD))
                }
            }
            if (tarefaOk(c, s)) add(label("Missão rápida concluída!", 20f, GOLD, true), 22)
            add(btn("Voltar ao caso") { hub() }, 24)
        }
    }

    private fun caderno() {
        val c = caso; val s = st
        val rot = listOf("? dúvida", "X descartado", "OK certeza")
        screen("caderno", { hub() }) {
            add(label("Caderno de hipóteses", 28f, CREAM, true), 0)
            add(label("Toque ou aperte Enter para mudar a anotação: ? , X ou OK.", 16f, MUTED))
            c.getJSONArray("suspeitos").objs().forEach { p ->
                val pid = p.getString("id")
                add(label(p.getString("nome"), 20f, GOLD, true), 22)
                add(card(p.getString("descricao")))
                add(btn("Anotação: " + rot[(s.notes[pid] ?: 0).coerceIn(0, 2)]) {
                    s.notes[pid] = ((s.notes[pid] ?: 0) + 1) % 3; save(); caderno()
                })
            }
            add(btn("Voltar ao caso") { hub() }, 24)
        }
    }

    private fun LinearLayout.pick(titulo: String, itens: List<JSONObject>, sel: String, set: (String) -> Unit) {
        add(label(titulo, 20f, GOLD, true), 22)
        itens.forEach { o ->
            val id = o.getString("id")
            add(btn((if (id == sel) "● " else "○ ") + o.getString("nome")) { set(id); save(); reuniao() })
        }
    }

    private fun reuniao() {
        val c = caso; val s = st
        if (s.closed) { desfecho(); return }
        val ok = c.getJSONArray("pistas").objs().all { it.getString("id") in s.analyzed } &&
                tarefaOk(c, s) && s.notices.size == c.getJSONArray("notificacoes").length()
        if (!ok) { hub(); return }
        val completa = s.quem.isNotEmpty() && s.arma.isNotEmpty() && s.onde.isNotEmpty()
        val r = c.getJSONObject("reuniao")
        screen("reuniao", { hub() }) {
            add(label("Reunião de emergência", 28f, CREAM, true), 0)
            add(label("Reúna as pistas e registre quem, qual técnica e em qual ambiente.", 16f, MUTED))
            pick("Quem?", c.getJSONArray("suspeitos").objs(), s.quem) { s.quem = it }
            pick("Qual técnica?", c.getJSONArray("armas").objs(), s.arma) { s.arma = it }
            pick("Em qual ambiente?", ambientes, s.onde) { s.onde = it }
            if (completa) {
                add(label(r.getString("pergunta"), 20f, GOLD, true), 26)
                r.getJSONArray("opcoes").objs().forEach { o ->
                    add(btn(o.getString("texto")) { s.choice = o.getString("id"); save(); desfecho() })
                }
            } else {
                add(label("Escolha as três partes para liberar o plano de proteção.", 15f, MUTED), 20)
            }
            add(btn("Voltar ao caso") { hub() }, 24)
        }
    }

    private fun desfecho() {
        val c = caso; val s = st
        val sol = c.getJSONObject("solucao")
        val certo = s.quem == sol.getString("quem") && s.arma == sol.getString("arma") && s.onde == sol.getString("onde")
        val op = c.getJSONObject("reuniao").getJSONArray("opcoes").objs().first { it.getString("id") == s.choice }
        val sus = c.getJSONArray("suspeitos").objs(); val arm = c.getJSONArray("armas").objs()
        screen("desfecho", { home() }) {
            add(label("Caso encerrado", 28f, CREAM, true), 0)
            add(label("Selo: " + c.getString("selo"), 22f, GOLD, true))
            figuras(180, "mascote_agente" to "Agente da Guarda Digital comemorando o fim do caso")
            if (certo) add(card("Hipótese certeira: quem, técnica e ambiente batem com as pistas.", GOLD))
            else add(card("Vamos pensar juntos. No roteiro: " + nome(sus, sol.getString("quem")) + "; " +
                    nome(arm, sol.getString("arma")) + "; " + nome(ambientes, sol.getString("onde")) + ".", GOLD))
            add(card(c.getString("fechamento")))
            add(card("Seu plano de proteção:\n" + op.getString("texto") + "\n\n" + op.getString("feedback")))
            val (seg, emp) = score()
            add(card("Pontos totais: Segurança $seg, Empatia $emp", GOLD))
            add(btn("Voltar aos casos", primary = true) { home() }, 20)
            add(btn("Reabrir este caso") {
                states[c.getString("id")] = CaseState(); save(); chamado()
            })
        }
    }
}
