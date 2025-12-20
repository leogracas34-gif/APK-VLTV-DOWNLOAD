package com.vltv.play

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SeriesDetailsActivity : AppCompatActivity() {

    private var seriesId: Int = 0
    private var seriesName: String = ""
    private var seriesIcon: String? = null
    private var seriesRating: String = "0.0"

    private lateinit var imgPoster: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvGenre: TextView
    private lateinit var tvPlot: TextView
    private lateinit var btnSeasonSelector: TextView
    private lateinit var rvEpisodes: RecyclerView
    private lateinit var btnFavoriteSeries: ImageButton

    private var episodesBySeason: Map<String, List<EpisodeStream>> = emptyMap()
    private var sortedSeasons: List<String> = emptyList()
    private var currentSeason: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series_details)

        seriesId = intent.getIntExtra("series_id", 0)
        seriesName = intent.getStringExtra("name") ?: ""
        seriesIcon = intent.getStringExtra("icon")
        seriesRating = intent.getStringExtra("rating") ?: "0.0"

        imgPoster = findViewById(R.id.imgPosterSeries)
        tvTitle = findViewById(R.id.tvSeriesTitle)
        tvRating = findViewById(R.id.tvSeriesRating)
        tvGenre = findViewById(R.id.tvSeriesGenre)
        tvPlot = findViewById(R.id.tvSeriesPlot)
        btnSeasonSelector = findViewById(R.id.btnSeasonSelector)
        rvEpisodes = findViewById(R.id.rvEpisodes)
        btnFavoriteSeries = findViewById(R.id.btnFavoriteSeries)

        tvTitle.text = seriesName
        tvRating.text = "Nota: $seriesRating"
        tvGenre.text = "Gênero: ..."
        tvPlot.text = "Sinopse..."

        btnSeasonSelector.setBackgroundColor(Color.parseColor("#333333"))

        Glide.with(this)
            .load(seriesIcon)
            .placeholder(R.mipmap.ic_launcher)
            .into(imgPoster)

        rvEpisodes.layoutManager = LinearLayoutManager(this)

        val isFavInicial = getFavSeries(this).contains(seriesId)
        atualizarIconeFavoritoSerie(isFavInicial)

        btnFavoriteSeries.setOnClickListener {
            val favs = getFavSeries(this)
            val novoFav: Boolean
            if (favs.contains(seriesId)) {
                favs.remove(seriesId)
                novoFav = false
            } else {
                favs.add(seriesId)
                novoFav = true
            }
            saveFavSeries(this, favs)
            atualizarIconeFavoritoSerie(novoFav)
        }

        btnSeasonSelector.setOnClickListener {
            mostrarSeletorDeTemporada()
        }

        carregarSeriesInfo()
    }

    // FAVORITOS

    private fun getFavSeries(context: Context): MutableSet<Int> {
        val prefs = context.getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        val set = prefs.getStringSet("fav_series", emptySet()) ?: emptySet()
        return set.mapNotNull { it.toIntOrNull() }.toMutableSet()
    }

    private fun saveFavSeries(context: Context, ids: Set<Int>) {
        val prefs = context.getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet("fav_series", ids.map { it.toString() }.toSet())
            .apply()
    }

    private fun atualizarIconeFavoritoSerie(isFav: Boolean) {
        val res = if (isFav) R.drawable.ic_star_filled else R.drawable.ic_star_border
        btnFavoriteSeries.setImageResource(res)
    }

    // CARREGAR INFO / EPISÓDIOS

    private fun carregarSeriesInfo() {
        val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("username", "") ?: ""
        val password = prefs.getString("password", "") ?: ""

        XtreamApi.service.getSeriesInfoV2(username, password, seriesId = seriesId)
            .enqueue(object : Callback<SeriesInfoResponse> {
                override fun onResponse(
                    call: Call<SeriesInfoResponse>,
                    response: Response<SeriesInfoResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        episodesBySeason = body.episodes ?: emptyMap()
                        sortedSeasons = episodesBySeason.keys.sortedBy { it.toIntOrNull() ?: 0 }

                        if (sortedSeasons.isNotEmpty()) {
                            mudarTemporada(sortedSeasons.first())
                        } else {
                            btnSeasonSelector.text = "Indisponível"
                        }
                    }
                }

                override fun onFailure(call: Call<SeriesInfoResponse>, t: Throwable) {
                    Toast.makeText(
                        this@SeriesDetailsActivity,
                        "Erro de conexão",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun mostrarSeletorDeTemporada() {
        if (sortedSeasons.isEmpty()) return

        val nomes = sortedSeasons.map { "Temporada $it" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Escolha a Temporada")
            .setItems(nomes) { _, which ->
                val seasonKey = sortedSeasons[which]
                mudarTemporada(seasonKey)
            }
            .show()
    }

    private fun mudarTemporada(seasonKey: String) {
        currentSeason = seasonKey
        btnSeasonSelector.text = "Temporada $seasonKey ▼"

        val lista = episodesBySeason[seasonKey] ?: emptyList()
        rvEpisodes.adapter = EpisodeAdapter(lista) { ep, position ->
            val streamId = ep.id.toIntOrNull() ?: 0
            val ext = ep.container_extension ?: "mp4"

            // Próximo episódio e nome dele
            val nextEp = lista.getOrNull(position + 1)
            val nextStreamId = nextEp?.id?.toIntOrNull() ?: 0
            val nextChannelName = nextEp?.let {
                "T${seasonKey}E${it.episode_num} - $seriesName"
            }

            val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
            val keyBase = "series_resume_$streamId"
            val pos = prefs.getLong("${keyBase}_pos", 0L)
            val dur = prefs.getLong("${keyBase}_dur", 0L)
            val existe = pos > 30_000L && dur > 0L && pos < (dur * 0.95).toLong()

            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("stream_id", streamId)
            intent.putExtra("stream_ext", ext)
            intent.putExtra("stream_type", "series")
            intent.putExtra(
                "channel_name",
                "T${seasonKey}E${ep.episode_num} - $seriesName"
            )
            if (existe) {
                intent.putExtra("start_position_ms", pos)
            }
            if (nextStreamId != 0) {
                intent.putExtra("next_stream_id", nextStreamId)
                if (nextChannelName != null) {
                    intent.putExtra("next_channel_name", nextChannelName)
                }
            }
            startActivity(intent)
        }
    }

    // ADAPTER EPISÓDIOS

    class EpisodeAdapter(
        private val list: List<EpisodeStream>,
        private val onClick: (EpisodeStream, Int) -> Unit
    ) : RecyclerView.Adapter<EpisodeAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvTitle: TextView = v.findViewById(R.id.tvEpisodeTitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_episode, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val ep = list[position]
            val epNum = ep.episode_num.toString().padStart(2, '0')
            holder.tvTitle.text = "E$epNum - ${ep.title}"
            holder.itemView.setOnClickListener { onClick(ep, position) }
        }

        override fun getItemCount() = list.size
    }
}
