from PIL import Image
import json
N=48
C={'K':(14,10,24),'F':(98,82,148),'f':(66,54,104),'l':(134,118,190),'M':(212,204,240),'m':(170,160,210),
   'W':(255,255,255),'E':(40,226,255),'e':(12,120,170),'R':(236,40,70),'r':(160,20,48),'T':(255,130,160),'N':(255,120,160),
   'D':(60,8,24),'B':(214,219,230),'b':(142,148,164),'P':(40,44,56),'p':(110,116,136),'Q':(255,170,200),'Y':(255,255,255)}
g=[['.']*N for _ in range(N)]
def put(x,y,c):
    if 0<=x<N and 0<=y<N: g[y][x]=c
def rect(x0,y0,x1,y1,c):
    for y in range(y0,y1+1):
        for x in range(x0,x1+1): put(x,y,c)
def inell(x,y,cx,cy,rx,ry): return ((x+.5-cx)/rx)**2+((y+.5-cy)/ry)**2<=1
def ell(cx,cy,rx,ry,c,cond=lambda x,y:True):
    for y in range(N):
        for x in range(N):
            if inell(x,y,cx,cy,rx,ry) and cond(x,y): put(x,y,c)
def tri(a,b,c_,col):
    def s(p,q,r): return (p[0]-r[0])*(q[1]-r[1])-(q[0]-r[0])*(p[1]-r[1])
    for y in range(N):
        for x in range(N):
            P=(x+.5,y+.5); d1,d2,d3=s(P,a,b),s(P,b,c_),s(P,c_,a)
            if not ((d1<0 or d2<0 or d3<0) and (d1>0 or d2>0 or d3>0)): put(x,y,col)
# ---- arm (behind the head): forearm up to the paw, big bicep, shoulder off the bottom edge
rect(37,12,43,21,'F'); rect(42,12,43,21,'f')
ell(41,25.5,5.6,5.2,'F'); ell(41,25.5,5.6,5.2,'f',lambda x,y: not inell(x,y,40.2,24.8,5,4.6))
for y in range(29,48): rect(37+(y-29)//6,y,44+(y-29)//5,y,'F'); put(44+(y-29)//5,y,'f')
put(38,24,'l'); put(39,23,'l'); put(40,23,'l')          # bicep shine
# ---- dumbbell held up
rect(32,7,47,8,'B'); rect(32,8,47,8,'b')
rect(29,2,32,13,'b'); rect(30,3,30,12,'B'); rect(33,4,34,11,'b'); put(33,5,'B')
rect(45,2,47,13,'b'); rect(46,3,46,12,'B'); rect(43,4,44,11,'b'); put(44,5,'B')
# paw wrapped around the bar
rect(36,4,42,11,'F'); put(36,4,'.'); put(42,4,'.'); rect(36,11,42,11,'f')
rect(38,4,38,7,'f'); rect(40,4,40,7,'f')                 # toes
rect(37,5,37,5,'l'); rect(39,5,39,5,'l'); rect(41,5,41,5,'l')
# ---- head
arm={(x,y) for y in range(N) for x in range(N) if g[y][x]!='.'}
ears=[((5,26),(9,6),(19,20)),((23,20),(31,6),(35,26))]
for a,b,c_ in ears: tri(a,b,c_,'F')
tri((8,24),(10,11),(16,20),'T'); tri((26,20),(30,11),(32,24),'T')
ell(20,32,17.5,14.5,'F')
ell(20,32,17.5,14.5,'f',lambda x,y: not inell(x,y,19,30.8,16.4,13.6))
head={(x,y) for y in range(N) for x in range(N) if inell(x,y,20,32,17.5,14.5)}
for (x,y) in head:
    if any((x+dx,y+dy) in arm and (x+dx,y+dy) not in head for dx,dy in ((1,0),(-1,0),(0,1),(0,-1))): put(x,y,'K')
# cheek fluff
for (x,y) in ((2,34),(2,35),(1,36),(2,37),(38,34),(38,35),(39,36),(38,37)): put(x,y,'F')
# muzzle
ell(20,38.5,8,5.2,'M'); ell(20,40,8,4,'m',lambda x,y: y>41)
# ---- bandana on top of the head, knot on the left with two tails
for y in range(N):
    for x in range(N):
        if g[y][x] in 'Ff' and 17<=y<=23 and 5<x<36 and inell(x,y,20,32,17.5,14.5): put(x,y,'R')
for x in range(6,36): 
    if g[23][x]=='R': put(x,23,'r')
for (x,y) in ((9,19),(14,18),(19,17),(24,18),(29,19),(12,21),(17,20),(22,20),(27,21),(32,21),(7,22)): 
    if g[y][x]=='R': put(x,y,'Y')
rect(2,21,5,24,'R'); put(2,21,'r'); put(5,24,'r'); put(3,22,'Y')               # knot
rect(0,25,2,29,'R'); rect(0,29,1,31,'R'); put(2,25,'r'); rect(3,25,4,27,'R'); put(4,27,'r')   # tails
# ---- eyes: big, cyan, slit pupils
for ex in (10,24):
    rect(ex,26,ex+6,32,'E'); rect(ex,26,ex+6,26,'K'); put(ex,27,'K') if ex==10 else None
    put(ex,32,'.'); put(ex+6,32,'.')
    rect(ex,31,ex+6,32,'e'); put(ex,32,'F'); put(ex+6,32,'F')
    rect(ex+3,27,ex+3,32,'K')                     # slit
    put(ex+1,27,'W'); put(ex+2,27,'W'); put(ex+1,28,'W')   # shine
# determined brows: inner ends lower
rect(10,25,13,25,'K'); put(14,25,'K'); put(15,26,'K'); put(16,26,'K')
rect(27,25,30,25,'K'); put(26,25,'K'); put(25,26,'K'); put(24,26,'K')
# ---- nose + mouth + triangle fangs
rect(19,35,21,35,'N'); put(20,36,'N')
put(20,37,'K'); put(19,38,'K'); put(18,38,'K'); put(21,38,'K'); put(22,38,'K')
rect(15,39,25,42,'D'); rect(15,39,25,39,'K'); put(14,39,'K'); put(26,39,'K'); put(14,40,'K'); put(26,40,'K'); rect(15,43,25,43,'K'); put(14,41,'K'); put(26,41,'K'); put(14,42,'K'); put(26,42,'K')
rect(19,42,21,42,'T')   # tongue
for fx in (15,22):      # fangs: 4 wide -> 2 -> point
    rect(fx,40,fx+3,40,'W'); rect(fx+1,41,fx+2,41,'W'); put(fx+1 if fx==15 else fx+2,42,'W')
# blush + whiskers
rect(6,36,8,36,'T'); rect(32,36,34,36,'T')

# (right whiskers hidden behind the arm)
# outline
o=[r[:] for r in g]
for y in range(N):
    for x in range(N):
        if g[y][x]=='.':
            if any(0<=x+dx<N and 0<=y+dy<N and g[y+dy][x+dx] not in '.Kl' for dx,dy in ((1,0),(-1,0),(0,1),(0,-1))): o[y][x]='K'
g=o
im=Image.new('RGBA',(N,N),(0,0,0,0))
for y in range(N):
    for x in range(N):
        if g[y][x]!='.': im.putpixel((x,y),C[g[y][x]]+(255,))
im.save('cat.png')
bg=Image.new('RGBA',(N*12,N*12),(6,8,10,255)); bg.alpha_composite(im.resize((N*12,N*12),Image.NEAREST)); bg.save('cat_preview.png')
