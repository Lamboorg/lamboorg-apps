from PIL import Image, ImageDraw, ImageFilter
import os
sp=Image.open('sprite.png')
A='/home/claude/lamboorg-apps'; R=A+'/android/app/src/main/res'; SITE='/home/claude/lamboorg/out'
def bgimg(S):
    b=Image.new('RGBA',(S,S),(6,8,10,255)); d=ImageDraw.Draw(b)
    glow=Image.new('RGBA',(S,S),(0,0,0,0)); ImageDraw.Draw(glow).ellipse((S*.18,S*.2,S*.82,S*.9),fill=(46,230,255,70)); b.alpha_composite(glow.filter(ImageFilter.GaussianBlur(S*.09)))
    step=S//40
    for x in range(0,S,step): d.line((x,0,x,S),fill=(255,255,255,7))
    for y in range(0,S,step): d.line((0,y,S,y),fill=(255,255,255,7))
    return b
def mascot(S, frac, floor=True, cy=0.5):
    """transparent canvas S, monster about frac of S wide, centered (cy = vertical center)"""
    im=Image.new('RGBA',(S,S),(0,0,0,0)); k=max(1,round(S*frac/40)); spr=sp.resize((40*k,40*k),Image.NEAREST)
    x=(S-40*k)//2; y=int(S*cy-20*k)
    if floor: ImageDraw.Draw(im).rectangle((x-2*k,y+39*k,x+42*k,y+39*k+max(1,k//2)),fill=(46,230,255,110))
    im.alpha_composite(spr,(x,y)); return im
def full(S, frac=0.72, cy=0.52):   # square, background, no rounding
    b=bgimg(1024); b.alpha_composite(mascot(1024,frac,cy=cy)); return b.resize((S,S),Image.LANCZOS)
def rounded(S, r=.22):
    im=full(S); m=Image.new('L',(S*4,S*4),0); ImageDraw.Draw(m).rounded_rectangle((0,0,S*4-1,S*4-1),radius=int(S*4*r),fill=255); im.putalpha(m.resize((S,S),Image.LANCZOS)); return im
def circle(S):
    im=full(S,0.64,0.53); m=Image.new('L',(S*4,S*4),0); ImageDraw.Draw(m).ellipse((0,0,S*4-1,S*4-1),fill=255); im.putalpha(m.resize((S,S),Image.LANCZOS)); return im
# site
full(180).save(SITE+'/icon-180.png'); rounded(192).save(SITE+'/icon-192.png'); rounded(512).save(SITE+'/icon-512.png'); full(512,0.56,0.53).save(SITE+'/icon-maskable.png')
for f in ('icon-180.png','icon-192.png','icon-512.png','icon-maskable.png'): Image.open(SITE+'/'+f).save(SITE+'/site/'+f)
# android
dens={'mdpi':1,'hdpi':1.5,'xhdpi':2,'xxhdpi':3,'xxxhdpi':4}
for d,f in dens.items():
    p=R+'/mipmap-'+d; os.makedirs(p,exist_ok=True)
    rounded(int(48*f),.18).save(p+'/ic_launcher.png'); circle(int(48*f)).save(p+'/ic_launcher_round.png')
    S=int(108*f)
    mascot(1024,0.50,cy=0.52).resize((S,S),Image.LANCZOS).save(p+'/ic_launcher_foreground.png')
    bgimg(1024).resize((S,S),Image.LANCZOS).save(p+'/ic_launcher_background.png')
    mo=mascot(1024,0.50,floor=False,cy=0.52); a=mo.getchannel('A'); w=Image.new('RGBA',mo.size,(255,255,255,0)); w.putalpha(a); w.resize((S,S),Image.LANCZOS).save(p+'/ic_launcher_monochrome.png')
mascot(1024,0.56,floor=False,cy=0.5).resize((432,432),Image.LANCZOS).save(R+'/drawable/splash_icon.png')
full(1024,0.70).save(A+'/desktop/build/icon.png')
print('done')
