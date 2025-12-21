package com.vltv.play

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetailsActivity : AppCompatActivity() {

    private lateinit var tvPlot: TextView
    private lateinit var tvGenre: TextView
    private lateinit var tvCast: TextView
    private lateinit var tvDirector: TextView
    private lateinit var tvRating: TextView
    private lateinit var imgPoster: ImageView
    private lateinit var btnPlay: Button
    private lateinit var btnFavorite: ImageButton
    private lateinit var btnResume: Button

    // área de download
    private lateinit var btnDownloadArea: LinearLayout
    private lateinit var imgDownloadState: ImageView
    private lateinit var tvDownloadState: TextView

    private var streamId: Int = 0
    private var extension: String = "mp4"

    private enum class DownloadState { BAIXAR, BAIXANDO, BAIXADO }
    private var downloadState: DownloadState = DownloadState.BAIXAR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        val name = intent.getStringExtra("title") ?: "Sem Título"
        val icon = intent.getStringExtra("icon")
        streamId = intent.getIntExtra("stream_id", 0)
        extension = intent.getStringExtra("extension") ?: "mp4"

        tvPlot = findViewById(R.id.tvPlot)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        tvGenre = findViewById(R.id.tvGenre)
        tvCast = findViewById(R.id.tvCast)
        tvDirector = findViewById(R.id.tvDirector)
        tvRating = findViewById(R.id.tvRating)
        imgPoster = findViewById(R.id.imgPoster)
        btnPlay = findViewById(R.id.btnPlay)
        btnFavorite = findViewById(R.id.btnFavorite)
        btnResume = findViewById(R.id.btnResume)

        btnDownloadArea = findViewById(R.id.btnDownloadArea)
        imgDownloadState = findViewById(R.id.imgDownloadState)
        tvDownloadState = findViewById(R.id.tvDownloadState)

        tvTitle.text = name

        Glide.with(this)
            .load(icon)
            .placeholder(R.mipmap.ic_launcher)
            .into(imgPoster)

        // FAVORITOS
        val isFavInicial = getFavMovies(this).contains(streamId)
        atualizarIconeFavorito(isFavInicial)

        btnFavorite.setOnClickListener {
            val favs = getFavMovies(this)
            val novoFav: Boolean
            if (favs.contains(streamId)) {
                favs.remove(streamId)
                novoFav = false
            } else {
                favs.add(streamId)
                novoFav = true
            }
            saveFavMovies(this, favs)
            atualizarIconeFavorito(novoFav)
        }

        // CONTINUAR ASSISTINDO
        configurarBotaoResume()

        btnPlay.setOnClickListener {
            abrirPlayer(name, startPositionMs = 0L)
        }
        btnPlay.requestFocus()

        btnResume.setOnClickListener {
            val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
            val keyBase = "movie_resume_$streamId"
            val pos = prefs.getLong("${keyBase}_pos", 0L)
            abrirPlayer(name, startPositionMs = pos)
        }

        // restaurar estado de download salvo
        restaurarEstadoDownload()

        btnDownloadArea.setOnClickListener {
            when (downloadState) {
                DownloadState.BAIXAR -> {
                    // depois aqui entra DownloadManager real
                    Toast.makeText(this, "Iniciando download de $name", Toast.LENGTH_SHORT).show()
                    setDownloadState(DownloadState.BAIXANDO)
                }
                DownloadState.BAIXANDO -> {
                    Toast.makeText(this, "Download em andamento...", Toast.LENGTH_SHORT).show()
                }
                DownloadState.BAIXADO -> {
                    Toast.makeText(this, "Abrir Meus downloads (em breve)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Detalhes do painel Xtream
        carregarDetalhes(streamId)
        // Completar com TMDB
        carregarDetalhesTmdb(name)
    }

    private fun abrirPlayer(name: String, startPositionMs: Long) {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("stream_id", streamId)
        intent.putExtra("stream_ext", extension)
        intent.putExtra("stream_type", "movie")
        intent.putExtra("channel_name", name)

        if (startPositionMs > 0L) {
            intent.putExtra("start_position_ms", startPositionMs)
        }
        startActivity(intent)
    }

    private fun configurarBotaoResume() {
        val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        val keyBase = "movie_resume_$streamId"
        val pos = prefs.getLong("${keyBase}_pos", 0L)
        val dur = prefs.getLong("${keyBase}_dur", 0L)

        val existe = pos > 30_000L && dur > 0L && pos < (dur * 0.95).toLong()
        btnResume.visibility = if (existe) Button.VISIBLE else Button.GONE
    }

    // -------- ESTADO DOWNLOAD VISUAL --------

    private fun setDownloadState(state: DownloadState) {
        downloadState = state
        val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("download_state_$streamId", state.name).apply()

        when (state) {
            DownloadState.BAIXAR -> {
                imgDownloadState.setImageResource(R.drawable.ic_dl_arrow)
                tvDownloadState.text = "Baixar"
            }
            DownloadState.BAIXANDO -> {
                // ícone de bolinha / loader, texto branco normal
                imgDownloadState.setImageResource(R.drawable.ic_dl_loading)
                tvDownloadState.text = "Baixando"
            }
            DownloadState.BAIXADO -> {
                imgDownloadState.setImageResource(R.drawable.ic_dl_done)
                tvDownloadState.text = "Baixado"
            }
        }
    }

    private fun restaurarEstadoDownload() {
        val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString("download_state_$streamId", DownloadState.BAIXAR.name)
        val state = try {
            DownloadState.valueOf(name ?: DownloadState.BAIXAR.name)
        } catch (_: Exception) {
            DownloadState.BAIXAR
        }
        setDownloadState(state)
    }

    // ------------ FAVORITOS ------------

    private fun getFavMovies(context: Context): MutableSet<Int> {
        val prefs = context.getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        val set = prefs.getStringSet("fav_movies", emptySet()) ?: emptySet()
        return set.mapNotNull { it.toIntOrNull() }.toMutableSet()
    }

    private fun saveFavMovies(context: Context, ids: Set<Int>) {
        val prefs = context.getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet("fav_movies", ids.map { it.toString() }.toSet())
            .apply()
    }

    private fun atualizarIconeFavorito(isFav: Boolean) {
        val res = if (isFav) R.drawable.ic_star_filled else R.drawable.ic_star_border
        btnFavorite.setImageResource(res)
    }

    // ------------ DETALHES VOD (Xtream) ------------

    private fun carregarDetalhes(streamId: Int) {
        val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("username", "") ?: ""
        val password = prefs.getString("password", "") ?: ""

        XtreamApi.service.getVodInfo(username, password, vodId = streamId)
            .enqueue(object : Callback<VodInfoResponse> {
                override fun onResponse(
                    call: Call<VodInfoResponse>,
                    response: Response<VodInfoResponse>
                ) {
                    if (response.isSuccessful && response.body()?.info != null) {
                        val info = response.body()!!.info!!

                        tvPlot.text = info.plot ?: "Sinopse indisponível."
                        tvGenre.text = "Gênero: ${info.genre ?: "N/A"}"
                        tvCast.text = "Elenco: ${info.cast ?: "N/A"}"
                        tvRating.text = "Nota: ${info.rating ?: "N/A"}"
                        tvDirector.text = "Diretor: ${info.director ?: "N/A"}"

                        if (!info.movie_image.isNullOrEmpty()) {
                            Glide.with(this@DetailsActivity)
                                .load(info.movie_image)
                                .into(imgPoster)
                        }
                    } else {
                        tvPlot.text = "Não foi possível carregar detalhes."
                    }
                }

                override fun onFailure(call: Call<VodInfoResponse>, t: Throwable) {
                    tvPlot.text = "Erro de conexão ao buscar detalhes."
                }
            })
    }

    // ------------ DETALHES TMDB ------------

    private fun carregarDetalhesTmdb(titulo: String) {
        val apiKey = TmdbConfig.API_KEY
        if (apiKey.isBlank()) return

        TmdbApi.service.searchMovie(apiKey, titulo)
            .enqueue(object : Callback<TmdbSearchResponse> {
                override fun onResponse(
                    call: Call<TmdbSearchResponse>,
                    response: Response<TmdbSearchResponse>
                ) {
                    val movie = response.body()?.results?.firstOrNull() ?: return

                    if (tvPlot.text.isNullOrBlank() || tvPlot.text == "Sinopse indisponível.") {
                        tvPlot.text = movie.overview ?: "Sinopse indisponível."
                    }

                    if (tvRating.text.isNullOrBlank() || tvRating.text.contains("N/A")) {
                        val nota = movie.vote_average ?: 0f
                        tvRating.text = "Nota: ${String.format("%.1f", nota)}"
                    }

                    if (movie.poster_path != null) {
                        val urlPoster = "https://image.tmdb.org/t/p/w500${movie.poster_path}"
                        Glide.with(this@DetailsActivity)
                            .load(urlPoster)
                            .into(imgPoster)
                    }
                }

                override fun onFailure(
                    call: Call<TmdbSearchResponse>,
                    t: Throwable
                ) {
                }
            })
    }
}
