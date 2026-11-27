from PIL import Image
import sys
N=40
C={'K':(8,18,14),'G':(78,226,118),'g':(36,156,78),'d':(24,110,56),'L':(178,255,196),'W':(255,255,255),'E':(40,226,255),'e':(10,120,170),
   'R':(255,60,96),'r':(186,28,62),'P':(30,33,42),'p':(96,102,122),'B':(214,219,230),'b':(142,148,164),'H':(252,240,206),'h':(204,184,140),'M':(110,16,36),'S':(150,240,255),'T':(255,120,150)}
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
# arms (behind body): upper arm with bicep bump, raised to the bar
for side in (0,1):
    m=(lambda x:x) if side==0 else (lambda x:N-1-x)
    for y in range(9,24):
        for x in (6,7,8): put(m(x),y,'G')
    for y in range(12,17): put(m(5),y,'G')      # bicep bulge outward
    for y in range(13,16): put(m(4),y,'G')
    for y in range(10,24): put(m(8),y,'g')
# body
arm={(x,y) for y in range(N) for x in range(N) if g[y][x] in 'Gg'}
ell(20,27.5,11.6,11.5,'G')
body={(x,y) for y in range(N) for x in range(N) if inell(x,y,20,27.5,11.6,11.5)}
ell(20,27.5,11.6,11.5,'g',lambda x,y: not inell(x,y,18.8,26.2,10.8,10.6))
ell(20,33.2,6,3.6,'L')
# a dark line where the arms meet the body
for (x,y) in body:
    if any((x+dx,y+dy) in arm and (x+dx,y+dy) not in body for dx,dy in ((1,0),(-1,0),(0,1),(0,-1))) and y<27: put(x,y,'d')
# feet
rect(11,37,15,38,'G'); rect(24,37,28,38,'G'); rect(13,38,15,38,'g'); rect(26,38,28,38,'g')
# horns on top of head
for side in (0,1):
    m=(lambda x:x) if side==0 else (lambda x:N-1-x)
    rect(m(13) if side==0 else m(14),14,m(14) if side==0 else m(13),17,'H')
    put(m(12),13,'H'); put(m(12),12,'H'); put(m(11),11,'h'); put(m(13),13,'H')
    put(m(14),17,'h')
# sweatband
for x in range(N):
    for y in (19,20):
        if g[y][x] in 'Gg' and 9<x<30: put(x,y,'R')
for x in range(12,29,4): put(x,20,'r'); put(x+1,19,'T')
# barbell (in front of fists' tops)
rect(4,6,35,7,'B'); rect(4,7,35,7,'b')
rect(0,1,3,12,'R'); rect(1,2,1,11,'T'); rect(3,1,3,12,'r'); rect(36,1,39,12,'R'); rect(37,2,37,11,'T'); rect(39,1,39,12,'r')
rect(4,3,4,10,'P'); rect(35,3,35,10,'P')
# fists gripping
for side in (0,1):
    m=(lambda x:x) if side==0 else (lambda x:N-1-x)
    for y in range(5,10):
        for x in range(5,10): put(m(x),y,'G')
    put(m(5),5,'.'); put(m(9),5,'.')
    for x in range(5,10): put(m(x),9,'g')
    put(m(7),6,'g'); put(m(7),7,'g')  # finger line
# eyes: big, glossy, determined
for ex in (13,23):
    rect(ex,23,ex+3,27,'W')
    rect(ex+1 if ex==13 else ex,24,ex+2 if ex==13 else ex+2,27,'E')
    rect(ex+1 if ex==13 else ex,26,ex+2 if ex==13 else ex+2,27,'e')
    put(ex+2 if ex==13 else ex+1,24,'W')
    put(ex,27,'W' ); put(ex+3,27,'W')
# brows angled down to the middle
rect(13,21,15,21,'K'); put(16,22,'K'); rect(24,21,26,21,'K'); put(23,22,'K')
# cheeks
put(11,28,'T'); put(12,28,'T'); put(27,28,'T'); put(28,28,'T')
# mouth: grin with two fangs
rect(16,30,23,31,'M'); rect(16,30,23,30,'K'); put(15,30,'K'); put(24,30,'K'); rect(16,32,23,32,'K'); put(15,31,'K'); put(24,31,'K')
put(17,31,'W'); put(22,31,'W'); rect(19,31,20,31,'T')
# sweat drop
put(29,14,'S'); put(29,15,'S'); put(30,15,'S'); put(29,16,'E'); put(30,16,'E')
# outline
o=[r[:] for r in g]
for y in range(N):
    for x in range(N):
        if g[y][x]=='.':
            if any(0<=x+dx<N and 0<=y+dy<N and g[y+dy][x+dx] not in '.K' for dx,dy in ((1,0),(-1,0),(0,1),(0,-1))): o[y][x]='K'
g=o
im=Image.new('RGBA',(N,N),(0,0,0,0))
for y in range(N):
    for x in range(N):
        if g[y][x]!='.': im.putpixel((x,y),C[g[y][x]]+(255,))
im.save('sprite.png')
import json; json.dump(g, open('grid.json','w'))
bg=Image.new('RGBA',(N*14,N*14),(5,5,6,255)); bg.alpha_composite(im.resize((N*14,N*14),Image.NEAREST)); bg.save('preview.png')
