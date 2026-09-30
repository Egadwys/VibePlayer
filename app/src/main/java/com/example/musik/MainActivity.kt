package com.example.musik

import android.Manifest.permission.*
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.session.*
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
 SideEffect{
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

@Composable fun Cover(s:Song?,m:Modifier=Modifier,shape:Shape=RoundedCornerShape(14.dp),px:Int=320,zoom:Float=1f){
 val c=LocalContext.current
 val bmp by produceState<Bitmap?>(s?.let{Covers.peek(it)},s?.id){value=if(s==null)null else Covers.get(c,s,px)}
 Box(m.clip(shape).aspectRatio(1f)){
  val b=bmp
  if(b!=null){val ib=remember(b){b.asImageBitmap()};Image(ib,null,Modifier.fillMaxSize().graphicsLayer(scaleX=zoom,scaleY=zoom),contentScale=ContentScale.Crop)}
  else Vinyl(Modifier.fillMaxSize())
 }
}

@Composable fun VisualizerWave(playing: Boolean) {
 if (!playing) return
 val infiniteTransition = rememberInfiniteTransition(label = "wave")
 val scale by infiniteTransition.animateFloat(
  initialValue = 1f, targetValue = 1.15f,
  animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s"
 )
 val alpha by infiniteTransition.animateFloat(
  initialValue = 0.5f, targetValue = 0f,
  animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a"
 )
 val color = MaterialTheme.colorScheme.primary
 Canvas(Modifier.size(280.dp)) {
  drawCircle(color = color.copy(alpha = alpha), radius = (size.minDimension / 2) * scale)
  drawCircle(color = color.copy(alpha = alpha * 0.4f), radius = (size.minDimension / 2) * (scale * 1.1f))
 }
}

@Composable fun <T> Grid(items:List<T>,key:(T)->Any,tile:@Composable (Int,T)->Unit){
 LazyVerticalGrid(GridCells.Fixed(3),Modifier.fillMaxSize(),contentPadding=PaddingValues(8.dp),
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
     color=if(active)MaterialTheme.colorScheme.primary else Color.Unspecified, textAlign = TextAlign.Start)
    Text(sub,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
   }
   if(menu.isNotEmpty())Box{
    IconButton({m=true},Modifier.size(28.dp)){Icon(Icons.Default.MoreVert,null,Modifier.size(18.dp))}
    DropdownMenu(m,{m=false}){menu.forEach{(t,a)->DropdownMenuItem(text={Text(t)},onClick={a();m=false})}}
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
 val pick=remember(g){g.mapValues{it.value.random()}}
 Grid(keys,{it}){_,k->Tile(pick[k],k,"${g.getValue(k).size} lagu"){open(k)}}
}

@Composable fun HomeTab(g:Map<String,List<Song>>,open:(String)->Unit){
 if(g.isEmpty())Box(Modifier.fillMaxSize(),Alignment.Center){Text("Tidak ada lagu di folder ini")} else Groups(g,open)
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
 if(dlg)AlertDialog(onDismissRequest={dlg=false},confirmButton={TextButton({pl.create(name.trim());name="";dlg=false}){Text("Buat")}},
  title={Text("Playlist baru")},text={OutlinedTextField(name,{name=it},singleLine=true,label={Text("Nama")})})
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable fun App(mode:Int,onTheme:()->Unit){
 val c=LocalContext.current
 val appSp = c.getSharedPreferences("AppState", Context.MODE_PRIVATE) 
 
 val savedUriString = appSp.getString("selected_folder_uri", null)
 var songs by remember {
  mutableStateOf(
   if (savedUriString != null) loadSongsFromFolder(c, Uri.parse(savedUriString))
   else loadSongs(c)
  )
 }

 val folderPickerLauncher = rememberLauncherForActivityResult(OpenDocumentTree()) { uri ->
  if (uri != null) {
   c.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
   appSp.edit().putString("selected_folder_uri", uri.toString()).apply()
   songs = loadSongsFromFolder(c, uri)
  }
 }

 val pl=remember{Playlists(c)}
 var ctrl by remember{mutableStateOf<MediaController?>(null)}
 var nowId by remember{mutableStateOf<String?>(null)}
 var playing by remember{mutableStateOf(false)}
 var shuffle by remember{mutableStateOf(false)}
 var repeat by remember{mutableIntStateOf(Player.REPEAT_MODE_OFF)}
 val pos=remember{mutableStateOf(0L)}
 var showNow by remember{mutableStateOf(false)}
 
 var tab by remember{mutableIntStateOf(appSp.getInt("tab", 0))}
 var detail by remember{mutableStateOf<String?>(appSp.getString("detail", null))}
 val pagerState = rememberPagerState(initialPage = tab) { 4 }

 LaunchedEffect(tab, detail) { appSp.edit().putInt("tab", tab).putString("detail", detail).apply() }
 LaunchedEffect(pagerState.settledPage) { tab = pagerState.settledPage }
 LaunchedEffect(tab) { if (pagerState.currentPage != tab) pagerState.animateScrollToPage(tab) }

 var restored by remember{mutableStateOf(false)}
 fun savePos(){
  if(!restored)return
  val p=ctrl?:return
  runCatching{
   val id=p.currentMediaItem?.mediaId?:return
   appSp.edit().putString("last_id",id).putLong("last_pos",p.currentPosition).apply()
  }
 }

 val lifecycleOwner = LocalLifecycleOwner.current
 DisposableEffect(lifecycleOwner) {
  val observer = LifecycleEventObserver { _, event ->
   if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) savePos()
  }
  lifecycleOwner.lifecycle.addObserver(observer)
  onDispose { savePos(); lifecycleOwner.lifecycle.removeObserver(observer) }
 }

 // simpan posisi saat pause / ganti lagu, dan tiap detik saat sedang diputar
 LaunchedEffect(playing, nowId) { savePos() }
 LaunchedEffect(playing, ctrl) { while(playing){ delay(1000); savePos() } }

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

 val byId=remember(songs){songs.associateBy{it.id}}
 
 LaunchedEffect(ctrl) {
  val p = ctrl ?: return@LaunchedEffect
  if (p.mediaItemCount == 0) {
   val lastId = appSp.getString("last_id", null)
   val lastSong = byId[lastId?.toLongOrNull() ?: 0L]
   if (lastSong != null) {
    val lastPos = appSp.getLong("last_pos", 0L)
    p.setMediaItem(lastSong.item(), lastPos)
    p.prepare()
   }
  }
  restored = true
 }

 BackHandler(showNow||detail!=null){if(showNow)showNow=false else detail=null}
 fun play(l:List<Song>,i:Int){ctrl?.run{setMediaItems(l.map{it.item()},i,0L);prepare();play()}}
 val cur=remember(byId,nowId){nowId?.toLongOrNull()?.let{byId[it]}}
 val byFolder=remember(songs){songs.filter{it.folder!=null}.groupBy{it.folder!!}}
 val byAlbum=remember(songs){songs.groupBy{it.album}}
 val byArtist=remember(songs){songs.groupBy{it.artist}}

 Box{
  Scaffold(topBar={
   TopAppBar(
    title={Text(detail?.drop(2)?:"VibeMusic",maxLines=1)},
    navigationIcon={if(detail!=null)IconButton({detail=null}){Icon(Icons.AutoMirrored.Filled.ArrowBack,null)}},
    actions={
     IconButton({ folderPickerLauncher.launch(null) }) {
      Icon(Icons.Default.FolderOpen, contentDescription = "Pilih Folder")
     }
     IconButton(onTheme){Icon(when(mode){0->Icons.Default.SettingsBrightness;1->Icons.Default.LightMode;else->Icons.Default.DarkMode},null)}
    }
   )
  },
  bottomBar={Column{
   if(cur!=null)Mini(cur,ctrl,playing,pos){showNow=true}
   FloatingNav(tab){tab=it;detail=null}
  }}){pad->
  
  if(detail!=null){
   val d = detail!!
   val n=d.drop(2)
   val ids=if(d[0]=='p')pl.map[n] else null
   val list=remember(d,songs,ids){when(d[0]){
    'f'->songs.filter{it.folder==n};'a'->songs.filter{it.album==n};'r'->songs.filter{it.artist==n}
    else->ids.orEmpty().mapNotNull{byId[it]}}}
   Box(Modifier.padding(pad).fillMaxSize().pointerInput(Unit){ 
    detectHorizontalDragGestures { _, dragAmount -> if (dragAmount > 40) detail = null }
   }) {
    SongGrid(list,nowId,pl,if(d[0]=='p')n else null){play(list,it)}
   }
  } else {
   HorizontalPager(state = pagerState, modifier = Modifier.padding(pad).fillMaxSize(), verticalAlignment = Alignment.Top) { page ->
    when(page){
     0->HomeTab(byFolder){detail="f:$it"}
     1->Groups(byAlbum){detail="a:$it"}
     2->Groups(byArtist){detail="r:$it"}
     else->PlaylistTab(pl,byId){detail="p:$it"}
    }
   }
  }
 }
 
 AnimatedVisibility(showNow,enter=slideInVertically(tween(300)){it},exit=slideOutVertically(tween(300)){it}){
  cur?.let{Now(it,ctrl,playing,pos,shuffle,repeat){showNow=false}}
 }
 }
}

@Composable fun FloatingNav(tab:Int,onSelect:(Int)->Unit){
 val items=listOf("Home" to Icons.Default.Home,"Album" to Icons.Default.Album,"Artis" to Icons.Default.Person,"Playlist" to Icons.AutoMirrored.Filled.QueueMusic)
 Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=24.dp,vertical=12.dp),contentAlignment=Alignment.Center){
  Surface(shape=RoundedCornerShape(50),color=MaterialTheme.colorScheme.surfaceContainerHigh,tonalElevation=0.dp,shadowElevation=10.dp){
   Row(Modifier.padding(8.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically){
    items.forEachIndexed{i,(t,ic)->
     val sel=tab==i
     Box(Modifier.width(68.dp).height(48.dp).clip(CircleShape)
      .background(if(sel)MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
      .clickable{onSelect(i)},contentAlignment=Alignment.Center){
      Icon(ic,t,tint=if(sel)MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
     }
    }
   }
  }
 }
}

@Composable fun Mini(s:Song,ctrl:MediaController?,playing:Boolean,pos:State<Long>,open:()->Unit){
 Surface(color=MaterialTheme.colorScheme.background,tonalElevation=0.dp,modifier=Modifier.clickable{open()}.pointerInput(Unit){
  detectVerticalDragGestures { _, dragAmount -> if (dragAmount < -30) open() }
 }){Column{
  LinearProgressIndicator(progress = { (pos.value.toFloat()/maxOf(s.dur,1L)).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth())
  Row(Modifier.padding(horizontal=12.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
   Cover(s,Modifier.size(44.dp))
   Column(Modifier.weight(1f).padding(horizontal=12.dp)){
    Text(s.title,maxLines=1,fontWeight=FontWeight.Bold,overflow=TextOverflow.Ellipsis, textAlign = TextAlign.Start)
    Text(s.artist,maxLines=1,style=MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start)
   }
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
 val idx by remember(s.id){derivedStateOf{lines?.indexOfLast{it.ms<=pos.value}?:-1}}
 val on=MaterialTheme.colorScheme.primary;val off=MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f)
 
 Surface(Modifier.fillMaxSize().pointerInput(Unit){
  detectVerticalDragGestures { _, dragAmount -> if (dragAmount > 40) close() }
 }){Box(Modifier.fillMaxSize()){
  Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal=24.dp)){
   
   Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
    VisualizerWave(playing) 
    Cover(s, Modifier.size(280.dp), shape = CircleShape, px = 700, zoom = 1.12f)
   }

   Box(Modifier.fillMaxWidth().height(100.dp), Alignment.CenterStart){
    val ll=lines
    when{
     ll==null->Text("Memuat lirik…",color=off, textAlign = TextAlign.Start)
     ll.isEmpty()->Text("Lirik tidak ditemukan",color=off, textAlign = TextAlign.Start)
     else->AnimatedContent(
      targetState = idx,
      transitionSpec = {
       (fadeIn(tween(400)) + slideInVertically(tween(400)){ it/2 }).togetherWith(
        fadeOut(tween(400)) + slideOutVertically(tween(400)){ -it/2 })
      }, label = "lyric"
     ){ i ->
      Text(ll.getOrNull(i)?.text?.ifBlank{"♪"}?:"", fontSize=22.sp, lineHeight=30.sp, 
       fontWeight=FontWeight.Bold, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
     }
    }
   }
   
   Spacer(Modifier.height(12.dp))
   Text(s.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
   Text(s.artist,color=on,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
   Spacer(Modifier.height(16.dp))
   
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
