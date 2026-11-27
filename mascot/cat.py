from PIL import Image
N=48
K=(12,12,14); Wc=(255,255,255)
g=[[None]*N for _ in range(N)]
def inell(x,y,cx,cy,rx,ry): return ((x+.5-cx)/rx)**2+((y+.5-cy)/ry)**2<=1
def tri_in(x,y,a,b,c):
    def s(p,q,r): return (p[0]-r[0])*(q[1]-r[1])-(q[0]-r[0])*(p[1]-r[1])
    P=(x+.5,y+.5); d1,d2,d3=s(P,a,b),s(P,b,c),s(P,c,a)
    return not ((d1<0 or d2<0 or d3<0) and (d1>0 or d2>0 or d3>0))
def part(test, fill='W'):
    m={(x,y) for y in range(N) for x in range(N) if test(x,y)}
    for (x,y) in m:
        edge=any((x+dx,y+dy) not in m and 0<=x+dx<N and y+dy<N for dx,dy in ((1,0),(-1,0),(0,1),(0,-1)))
        if edge or (x in (0,N-1) and y<N-1): g[y][x]='K'
        else: g[y][x]=fill
    return m
def px(x,y,c):
    if 0<=x<N and 0<=y<N: g[y][x]=c
# dumbbell (back), fully in frame
part(lambda x,y: 31<=x<=45 and 7<=y<=9)
part(lambda x,y: 28<=x<=32 and 2<=y<=14 and (x,y) not in ((28,2),(32,2),(28,14),(32,14)))
part(lambda x,y: 43<=x<=47 and 2<=y<=14 and (x,y) not in ((43,2),(47,2),(43,14),(47,14)))
# arm: shoulder off the bottom, bicep, forearm
part(lambda x,y: (y>=30 and 36+(y-30)//5<=x<=44+(y-30)//6) or inell(x,y,40.5,25.5,5.6,5.4) or (35<=x<=41 and 11<=y<=24))
# paw on the bar
part(lambda x,y: 34<=x<=41 and 3<=y<=12 and (x,y) not in ((34,3),(41,3),(34,12),(41,12)))
for y in (3,4,5): px(36,y,'K'); px(39,y,'K')
px(41,22,'K'); px(42,23,'K')   # bicep line
# ears + head (round, big)
part(lambda x,y: tri_in(x,y,(5,25),(9,8),(19,19)))
part(lambda x,y: tri_in(x,y,(23,19),(31,8),(35,25)))
head=part(lambda x,y: inell(x,y,20,32.5,17,14.6))
# inner ear lines
for (x,y) in ((9,13),(9,14),(10,15),(11,16),(30,13),(30,14),(29,15),(28,16)): px(x,y,'K')
# bandana: black band over the top of the head, white dots, knot + tails on the left
for (x,y) in head:
    if y<=24 and g[y][x]=='W': g[y][x]='K'
for (x,y) in ((11,21),(16,19),(21,19),(26,20),(31,22),(13,23),(19,22),(24,22),(29,23),(8,23),(17,24),(27,24)):
    if g[y][x]=='K' and (x,y) in head and y>18: g[y][x]='W'
for y in range(20,25):
    for x in range(1,6): px(x,y,'K')
for y in range(25,31): px(1,y,'K'); px(2,y,'K')
for y in range(25,28): px(4,y,'K'); px(5,y,'K')
px(3,22,'W')
# face: big shiny eyes
for cx in (13,27):
    for y in range(N):
        for x in range(N):
            if inell(x,y,cx,30.5,3.1,3.8): g[y][x]='K'
    px(cx-2,28,'W'); px(cx-1,28,'W'); px(cx-2,29,'W'); px(cx-1,29,'W'); px(cx+1,32,'W')
# blush ///
for bx in (8,30):
    for i in range(3): px(bx+i*2,36,'K'); px(bx+i*2+1,35,'K')
# nose + w mouth + two little fangs
for x in (19,20,21): px(x,34,'K')
px(20,35,'K')
for (x,y) in ((16,36),(17,37),(18,37),(19,37),(20,36),(21,37),(22,37),(23,37),(24,36)): px(x,y,'K')
for fx in (17,22):
    px(fx,38,'K'); px(fx+1,38,'W'); px(fx+2,38,'K'); px(fx+1,39,'K')
# whiskers

im=Image.new('RGBA',(N,N),Wc+(255,))
for y in range(N):
    for x in range(N):
        if g[y][x]=='K': im.putpixel((x,y),K+(255,))
im.save('cute.png')
im.resize((N*12,N*12),Image.NEAREST).save('cute_preview.png')
