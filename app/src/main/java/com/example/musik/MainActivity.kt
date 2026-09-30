package com.example.musik
import android.Manifest.permission.*
import android.app.Activity
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import androidx.media3.session.*
import kotlin.math.max
import kotlinx.coroutines.delay

class MainActivity:ComponentActivity(){
 override fun onCreate(b:Bundle?){super.onCreate(b);enableEdgeToEdge();setContent{Root()}}
}

@Composable fun MusikTheme(mode:Int,content:@Composable () -> Unit){
 val dark=mode==2||(mode==0&&isSystemInDarkTheme())
 val cs=if(dark)darkColorScheme(primary=Color(0xFF80CBC4),background=Color.Black,surface=Color.Black,
  surfaceVariant=Color(0xFF161616),surfaceContainer=Color(0xFF0C0C0C))
 else lightColorScheme(primary=Color(0xFF00796B))
 val view=LocalView.current
 SideEffect{ // ikon status bar & navigation bar mengikuti tema aplikasi (hitam di mode terang)
  (view.context as? Activity)?.window?.let{w->
   val ic=WindowCompat.getInsetsController(w,view)
   ic.isAppearanceLightStatusBars=!dark;ic.isAppearanceLightNavigationBars=!dark
  }
 }
 MaterialTheme(cs,content=content)
}

@Composable fun Root(){
 val sp=LocalContext.current.getSharedPreferences("s",0)
 var mode by remember{mutableIntStateOf(sp.getInt("mode",0))}
 MusikTheme(mode){Surface(Modifier.fillMaxSize()){Perm{App(mode){mode=(mode+1)%3;sp.edit().putInt("mode",mode).apply()}}}}
}

@Composable fun Perm(content:@Composable () -> Unit){
 val c=LocalContext.current
 val need=if(Build.VERSION.SDK_INT>=33)arrayOf(READ_MEDIA_AUDIO,POST_NOTIFICATIONS) else arrayOf(READ_EXTERNAL_STORAGE)
 fun ok()=ContextCompat.checkSelfPermission(c,need[0])==PackageManager.PERMISSION_GRANTED
 var granted by remember{mutableStateOf(ok())}
 val l=rememberLauncherForActivityResult(RequestMultiplePermissions()){granted=ok()}
 LaunchedEffect(Unit){if(!granted)l.launch(need)}
 if(granted)content() else Box(Modifier.fillMaxSize(),Alignment.Center){Button({l.launch(need)}){Text("Izinkan akses musik")}}
}

fun fmt(ms:Long)="%d:%02d".format(ms/60000,ms/1000%60)

// ---------- Cover & visual ----------
@Composable fun Vinyl(m:Modifier=Modifier){
 val bg=MaterialTheme.colorScheme.surfaceVariant
 Canvas(m.background(bg)){
  val r=size.minDimension*0.42f
  drawCircle(Color(0xFF0B0B0B),r,center)
  listOf(0.85f,0.68f,0.52f).forEach{drawCircle(Color(0x44FFFFFF),r*it,center,style=Stroke(size.minDimension*0.006f))}
  drawCircle(Color(0xFF26A69A),r*0.34f,center)
  drawCircle(Color(0xFF0B0B0B),r*0.07f,center)
 }
}

@Composable fun Cover(s:Song?,m:Modifier=Modifier){
 val c=LocalContext.current
 val bmp by produceState<Bitmap?>(s?.let{Covers.peek(it)},s?.id){value=if(s==null)null else Covers.get(c,s,320)}
 Box(m.clip(RoundedCornerShape(14.dp)).aspectRatio(1f)){
  val b=bmp
  if(b!=null){val ib=remember(b){b.asImageBitmap()};Image(ib,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)}
  else Vinyl(Modifier.fillMaxSize())
 }
}

@Composable fun FullCover(s:Song,m:Modifier){
 val c=LocalContext.current
 val bmp by produceState<Bitmap?>(Covers.peek(s),s.id){value=Covers.get(c,s,1024)}
 val b=bmp
 if(b!=null){val ib=remember(b){b.asImageBitmap()};Image(ib,null,m,contentScale=ContentScale.Crop,alignment=Alignment.TopCenter)}
 else Box(m,Alignment.TopCenter){Vinyl(Modifier.fillMaxWidth().aspectRatio(1f))}
}

/** Visualizer asli: spektrum audio yang sedang diputar. Warna mengikuti tema. */
@Composable fun Bars(active:Boolean,m:Modifier){
 var tick by remember{mutableLongStateOf(0L)}
 val d=remember{FloatArray(Spectrum.B)}
 LaunchedEffect(Unit){while(true){withFrameNanos{tick=it}}}
 val col=MaterialTheme.colorScheme.onSurface
 Canvas(m){
  val tk=tick
  val lv=Spectrum.levels
  val n=Spectrum.B;val gap=size.width/n;val w=gap*0.55f
  for(i in 0 until n){
   d[i]=if(active)max(lv[i],d[i]*0.85f) else d[i]*0.85f
   val h=size.height*(0.08f+0.92f*d[i])
   drawRoundRect(col,Offset(i*gap+(gap-w)/2,(size.height-h)/2),Size(w,h),CornerRadius(w/2))
  }
 }
}

// ---------- Grid 3 kolom ----------
@Composable fun <T> Grid(items:List<T>,key:(T)->Any,tile:@Composable (Int,T)->Unit){
 LazyVerticalGrid(GridCells.Fixed(3),contentPadding=PaddingValues(8.dp),
  horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
  itemsIndexed(items,key={_,t->key(t)}){i,t->tile(i,t)}
 }
}

@Composable fun Tile(cover:Song?,title:String,sub:String,menu:List<Pair<String,()->Unit>> = emptyList(),active:Boolean=false,onClick:()->Unit){
 var m by remember{mutableStateOf(false)}
 Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick=onClick).padding(4.dp)){
  Cover(cover,Modifier.fillMaxWidth())
  Row(verticalAlignment=Alignment.CenterVertically){
   Column(Modifier.weight(1f).padding(top=6.dp)){
    Text(title,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelLarge,
     color=if(active)MaterialTheme.colorScheme.primary else Color.Unspecified)
    Text(sub,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   if(menu.isNotEmpty())Box{
    IconButton({m=true},Modifier.size(28.dp)){Icon(Icons.Default.MoreVert,null,Modifier.size(18.dp))}
    DropdownMenu(m,{m=false}){menu.forEach{(t,a)->DropdownMenuItem({Text(t)},{a();m=false})}}
   }
  }
 }
}

@Composable fun SongGrid(list:List<Song>,nowId:String?,pl:Playlists,from:String?,onPlay:(Int)->Unit){
 val names=pl.map.keys.sorted()
 Grid(list,{it.id}){i,s->
  val menu:List<Pair<String,()->Unit>> = names.map{n->"Tambah ke $n" to {pl.add(n,s.id)}}+
   (if(from!=null)listOf("Hapus dari playlist" to {pl.remove(from,s.id)}) else emptyList())
  Tile(s,s.title,s.artist,menu,s.id.toString()==nowId){onPlay(i)}
 }
}

@Composable fun Groups(g:Map<String,List<Song>>,open:(String)->Unit){
 val keys=remember(g){g.keys.sorted()}
 val pick=remember(g){g.mapValues{it.value.random()}} // cover acak, dipilih ulang tiap halaman dibuka
 Grid(keys,{it}){_,k->Tile(pick[k],k,"${g.getValue(k).size} lagu"){open(k)}}
}

@Composable fun HomeTab(g:Map<String,List<Song>>,open:(String)->Unit){
 if(g.isEmpty())Box(Modifier.fillMaxSize(),Alignment.Center){Text("Tidak ada lagu di folder Music")} else Groups(g,open)
}

@Composable fun PlaylistTab(pl:Playlists,byId:Map<Long,Song>,open:(String)->Unit){
 var dlg by remember{mutableStateOf(false)};var name by remember{mutableStateOf("")}
 Column{
  FilledTonalButton({dlg=true},Modifier.padding(horizontal=12.dp)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("Playlist baru")}
  Grid(pl.map.keys.sorted(),{it}){_,n->
   val ids=pl.map[n].orEmpty()
   Tile(ids.firstNotNullOfOrNull{byId[it]},n,"${ids.size} lagu",listOf("Hapus" to {pl.delete(n)})){open(n)}
  }
 }
 if(dlg)AlertDialog({dlg=false},confirmButton={TextButton({pl.create(name.trim());name="";dlg=false}){Text("Buat")}},
  title={Text("Playlist baru")},text={OutlinedTextField(name,{name=it},singleLine=true,label={Text("Nama")})})
}

// ---------- App ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun App(mode:Int,onTheme:()->Unit){
 val c=LocalContext.current
 var songs by remember{mutableStateOf(loadSongs(c))}
 val pl=remember{Playlists(c)}
 var ctrl by remember{mutableStateOf<MediaController?>(null)}
 var nowId by remember{mutableStateOf<String?>(null)}
 var playing by remember{mutableStateOf(false)}
 var shuffle by remember{mutableStateOf(false)}
 var repeat by remember{mutableIntStateOf(Player.REPEAT_MODE_OFF)}
 val pos=remember{mutableStateOf(0L)} // hanya dibaca Mini/Now, agar App tidak recompose tiap 250ms
 var showNow by remember{mutableStateOf(false)}
 var tab by remember{mutableIntStateOf(0)}
 var detail by remember{mutableStateOf<String?>(null)}

 // Metadata dari file dulu; yang masih kosong disinkronkan dari internet (batch, di-cache)
 LaunchedEffect(Unit){
  val upd=HashMap<Long,Song>();var n=0
  for(s in songs.toList()){
   if((s.artist==UA||s.album==UB)&&!Meta.tried(c,s.id)){
    val r=Meta.fetch(c,s)
    if(r!=null&&r!=s){upd[s.id]=r;n++}
    if(n>=15){val u=HashMap(upd);songs=songs.map{u[it.id]?:it};n=0}
    delay(1500)
   }
  }
  if(upd.isNotEmpty())songs=songs.map{upd[it.id]?:it}
 }
 DisposableEffect(Unit){
  val f=MediaController.Builder(c,SessionToken(c,ComponentName(c,PlaybackService::class.java))).buildAsync()
  f.addListener({ctrl=f.get()},ContextCompat.getMainExecutor(c))
  onDispose{MediaController.releaseFuture(f)}
 }
 DisposableEffect(ctrl){
  val p=ctrl?:return@DisposableEffect onDispose{}
  nowId=p.currentMediaItem?.mediaId;playing=p.isPlaying;shuffle=p.shuffleModeEnabled;repeat=p.repeatMode
  val l=object:Player.Listener{override fun onEvents(q:Player,e:Player.Events){
   nowId=q.currentMediaItem?.mediaId;playing=q.isPlaying;shuffle=q.shuffleModeEnabled;repeat=q.repeatMode}}
  p.addListener(l);onDispose{p.removeListener(l)}
 }
 LaunchedEffect(ctrl){while(true){pos.value=ctrl?.currentPosition?:0L;delay(250)}}
 BackHandler(showNow||detail!=null){if(showNow)showNow=false else detail=null}

 fun play(l:List<Song>,i:Int){ctrl?.run{setMediaItems(l.map{it.item()},i,0L);prepare();play()}}
 val byId=remember(songs){songs.associateBy{it.id}}
 val cur=remember(byId,nowId){nowId?.toLongOrNull()?.let{byId[it]}}
 val byFolder=remember(songs){songs.filter{it.folder!=null}.groupBy{it.folder!!}}
 val byAlbum=remember(songs){songs.groupBy{it.album}}
 val byArtist=remember(songs){songs.groupBy{it.artist}}

 Box{
 Scaffold(topBar={TopAppBar(title={Text(detail?.drop(2)?:"VibeMusic",maxLines=1)},
   navigationIcon={if(detail!=null)IconButton({detail=null}){Icon(Icons.AutoMirrored.Filled.ArrowBack,null)}},
   actions={IconButton(onTheme){Icon(when(mode){0->Icons.Default.SettingsBrightness;1->Icons.Default.LightMode;else->Icons.Default.DarkMode},null)}})},
  bottomBar={Column{
   if(cur!=null)Mini(cur,ctrl,playing,pos){showNow=true}
   NavigationBar{
    listOf("Home" to Icons.Default.Home,"Album" to Icons.Default.Album,"Artis" to Icons.Default.Person,"Playlist" to Icons.AutoMirrored.Filled.QueueMusic)
     .forEachIndexed{i,(t,ic)->NavigationBarItem(tab==i,{tab=i;detail=null},{Icon(ic,null)},label={Text(t)})}
   }}}){pad->
  Crossfade(Pair(tab,detail),Modifier.padding(pad).fillMaxSize(),tween(180),label="page"){(t,d)->
   if(d!=null){
    val n=d.drop(2)
    val ids=if(d[0]=='p')pl.map[n] else null
    val list=remember(d,songs,ids){when(d[0]){
     'f'->songs.filter{it.folder==n};'a'->songs.filter{it.album==n};'r'->songs.filter{it.artist==n}
     else->ids.orEmpty().mapNotNull{byId[it]}}}
    SongGrid(list,nowId,pl,if(d[0]=='p')n else null){play(list,it)}
   }else when(t){
    0->HomeTab(byFolder){detail="f:$it"}
    1->Groups(byAlbum){detail="a:$it"}
    2->Groups(byArtist){detail="r:$it"}
    else->PlaylistTab(pl,byId){detail="p:$it"}
   }
  }
 }
 AnimatedVisibility(showNow,enter=fadeIn(tween(200)),exit=fadeOut(tween(150))){
  cur?.let{Now(it,ctrl,playing,pos,shuffle,repeat){showNow=false}}
 }
 }
}

@Composable fun Mini(s:Song,ctrl:MediaController?,playing:Boolean,pos:State<Long>,open:()->Unit){
 Surface(tonalElevation=3.dp,modifier=Modifier.clickable{open()}){Column{
  LinearProgressIndicator({(pos.value.toFloat()/maxOf(s.dur,1L)).coerceIn(0f,1f)},Modifier.fillMaxWidth())
  Row(Modifier.padding(horizontal=12.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
   Cover(s,Modifier.size(44.dp))
   Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(s.title,maxLines=1,fontWeight=FontWeight.Bold,overflow=TextOverflow.Ellipsis);Text(s.artist,maxLines=1,style=MaterialTheme.typography.bodySmall)}
   IconButton({ctrl?.seekToPrevious()}){Icon(Icons.Default.SkipPrevious,null)}
   IconButton({if(playing)ctrl?.pause() else ctrl?.play()}){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,null)}
   IconButton({ctrl?.seekToNext()}){Icon(Icons.Default.SkipNext,null)}
  }}}
}

@Composable fun Seek(pos:State<Long>,dur:Long,ctrl:MediaController?){
 val d=maxOf(dur,1L).toFloat()
 val p=pos.value
 Slider(p.toFloat().coerceIn(0f,d),{ctrl?.seekTo(it.toLong())},valueRange=0f..d)
 Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text(fmt(p));Text(fmt(dur))}
}

@Composable fun Now(s:Song,ctrl:MediaController?,playing:Boolean,pos:State<Long>,shuffle:Boolean,repeat:Int,close:()->Unit){
 var lines by remember(s.id){mutableStateOf<List<Line>?>(null)}
 LaunchedEffect(s.id){lines=Lyrics.get(s)}
 val ls=rememberLazyListState()
 val idx by remember(s.id){derivedStateOf{lines?.indexOfLast{it.ms<=pos.value}?:-1}}
 LaunchedEffect(idx){if(idx>=0)ls.animateScrollToItem(maxOf(idx-3,0))}
 val bg=MaterialTheme.colorScheme.background
 val on=MaterialTheme.colorScheme.primary;val off=MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f)
 val top=WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
 Surface(Modifier.fillMaxSize()){Box(Modifier.fillMaxSize()){
  FullCover(s,Modifier.fillMaxSize())
  Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to bg.copy(alpha=0.35f),0.45f to bg.copy(alpha=0.65f),1f to bg.copy(alpha=0.98f))))
  Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal=24.dp)){
   Box(Modifier.weight(1f).fillMaxWidth()){
    val ll=lines
    when{
     ll==null->Text("Memuat lirik…",Modifier.align(Alignment.Center))
     ll.isEmpty()->Text("Lirik tersinkron tidak ditemukan",Modifier.align(Alignment.Center))
     else->LazyColumn(state=ls,contentPadding=PaddingValues(top=top+56.dp,bottom=24.dp)){itemsIndexed(ll){i,l->
      Text(l.text.ifBlank{"♪"},fontSize=20.sp,lineHeight=28.sp,textAlign=TextAlign.Center,
       fontWeight=if(i==idx)FontWeight.Bold else FontWeight.Normal,
       color=MaterialTheme.colorScheme.onSurface.copy(alpha=if(i==idx)1f else 0.5f),
       modifier=Modifier.fillMaxWidth().clickable{ctrl?.seekTo(l.ms)}.padding(vertical=6.dp))}}
    }
   }
   Bars(playing,Modifier.fillMaxWidth().height(48.dp))
   Spacer(Modifier.height(8.dp))
   Text(s.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.fillMaxWidth())
   Text(s.artist,color=on,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.fillMaxWidth())
   Seek(pos,s.dur,ctrl)
   Row(Modifier.fillMaxWidth().padding(bottom=16.dp),Arrangement.SpaceEvenly,Alignment.CenterVertically){
    IconButton({ctrl?.let{it.shuffleModeEnabled=!it.shuffleModeEnabled}}){Icon(Icons.Default.Shuffle,null,tint=if(shuffle)on else off)}
    IconButton({ctrl?.seekToPrevious()},Modifier.size(48.dp)){Icon(Icons.Default.SkipPrevious,null,Modifier.size(32.dp))}
    FilledIconButton({if(playing)ctrl?.pause() else ctrl?.play()},Modifier.size(72.dp)){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,null,Modifier.size(40.dp))}
    IconButton({ctrl?.seekToNext()},Modifier.size(48.dp)){Icon(Icons.Default.SkipNext,null,Modifier.size(32.dp))}
    IconButton({ctrl?.let{it.repeatMode=when(it.repeatMode){Player.REPEAT_MODE_OFF->Player.REPEAT_MODE_ALL;Player.REPEAT_MODE_ALL->Player.REPEAT_MODE_ONE;else->Player.REPEAT_MODE_OFF}}}){
     Icon(if(repeat==Player.REPEAT_MODE_ONE)Icons.Default.RepeatOne else Icons.Default.Repeat,null,tint=if(repeat==Player.REPEAT_MODE_OFF)off else on)}
   }
  }
  IconButton(close,Modifier.align(Alignment.TopStart).statusBarsPadding().padding(4.dp)){Icon(Icons.Default.KeyboardArrowDown,null)}
 }}
}
