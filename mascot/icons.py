from PIL import Image, ImageDraw
import os
sp=Image.open('cute.png').convert('RGBA'); Z=sp.size[0]
A='/home/claude/lamboorg-apps'; R=A+'/android/app/src/main/res'; SITE='/home/claude/lamboorg/out'
def art(S, frac, cy):
    im=Image.new('RGBA',(S,S),(255,255,255,255)); k=max(1,round(S*frac/Z)); spr=sp.resize((Z*k,Z*k),Image.NEAREST)
    x=(S-Z*k)//2; y=int(S*cy-Z*k/2)
    if y+Z*k>S: spr=spr.crop((0,0,Z*k,S-y))
    im.paste(spr,(x,y)); return im
def full(S, frac=0.9, cy=0.56): return art(1024,frac,cy).resize((S,S),Image.LANCZOS)
def masked(im, shape, r=.22):
    S=im.size[0]; m=Image.new('L',(S*4,S*4),0); d=ImageDraw.Draw(m)
    if shape=='circle': d.ellipse((0,0,S*4-1,S*4-1),fill=255)
    else: d.rounded_rectangle((0,0,S*4-1,S*4-1),radius=int(S*4*r),fill=255)
    im=im.copy(); im.putalpha(m.resize((S,S),Image.LANCZOS)); return im
full(180).save(SITE+'/icon-180.png'); masked(full(192),'r').save(SITE+'/icon-192.png'); masked(full(512),'r').save(SITE+'/icon-512.png'); full(512,0.7,0.53).save(SITE+'/icon-maskable.png')
for f in ('icon-180.png','icon-192.png','icon-512.png','icon-maskable.png'): Image.open(SITE+'/'+f).save(SITE+'/site/'+f)
for d,f in {'mdpi':1,'hdpi':1.5,'xhdpi':2,'xxhdpi':3,'xxxhdpi':4}.items():
    p=R+'/mipmap-'+d
    masked(full(int(48*f)),'r',.18).save(p+'/ic_launcher.png'); masked(full(int(48*f),0.86,0.55),'circle').save(p+'/ic_launcher_round.png')
    S=int(108*f); fg=art(1024,0.64,0.52)
    fg.resize((S,S),Image.LANCZOS).save(p+'/ic_launcher_foreground.png')
    Image.new('RGBA',(S,S),(255,255,255,255)).save(p+'/ic_launcher_background.png')
    a=fg.convert('L').point(lambda v: 255 if v<128 else 0); w=Image.new('RGBA',fg.size,(255,255,255,0)); w.putalpha(a); w.resize((S,S),Image.LANCZOS).save(p+'/ic_launcher_monochrome.png')
# Android 12 splash: the cat on a white circle (the system shows it in the middle of the dark splash)
sp_img=Image.new('RGBA',(1152,1152),(0,0,0,0)); c=masked(art(768,0.86,0.55),'circle'); sp_img.alpha_composite(c,(192,192)); sp_img.save(R+'/drawable/splash_icon.png')
full(1024).save(A+'/desktop/build/icon.png')
sheet=Image.new('RGBA',(1100,560),(40,44,52,255))
sheet.alpha_composite(Image.open(SITE+'/icon-512.png'),(20,24))
sheet.alpha_composite(Image.open(R+'/mipmap-xxxhdpi/ic_launcher_round.png').resize((192,192)),(560,30))
sheet.alpha_composite(Image.open(SITE+'/icon-192.png').resize((96,96)),(780,78)); sheet.alpha_composite(Image.open(SITE+'/icon-192.png').resize((48,48)),(900,102))
fgx=Image.open(R+'/mipmap-xxxhdpi/ic_launcher_foreground.png'); s=fgx.size[0]; ad=masked(fgx.crop((int(s*.167),)*2+(int(s*.833),)*2).resize((192,192)),'circle'); sheet.alpha_composite(ad,(560,260))
sheet.save('cute_sheet.png'); print('ok')
