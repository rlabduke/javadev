package boundrotamers;

interface Version { static final String VERSION = "0.69"; static final String BUILD = "0_0"; }
/*
==============================================================================
===  TO DO  ==================================================================
==============================================================================
Make Rota1D write constant contours correctly again...

==============================================================================
===  CHANGE LOG  =============================================================
==============================================================================

### 0.69 ###
- added NdftVertexSource and support for the util.isosurface package
- updated Rota3D to use util.isosurface.MeshMarchingCubes to do its job
  results in ~50% as many lines in the kinemage with as good or better contours!

### 0.68 ###
- slight modification to NDFloatTable.fractionLessThan() to eliminate
  negative zeros in .data output files.

### 0.67 ###
- using clash cutoffs for 4-D data, before B-filtering:
  Lys: 6399 -> 4214 (loss of 34%)
  Arg: 4926 -> 2988 (loss of 39%)
- after B-filtering for B < 40:
  Lys: 4130 -> 2946 (loss of 29%, loss of 54% from raw)
  Arg: 3647 -> 2474 (loss of 32%, loss of 50% from raw)
- after B-filtering for B < 30:
  Lys: 2576 -> 1937 (loss of 25%, loss of 70% from raw)
  Arg: 2822 -> 1998 (loss of 29%, loss of 59% from raw)
- fixed Rota3D to write both constant and dd contours
- fixed Rota2D to write both constant and dd contours
- experiments to compare and constrast density-dependent smoothing
  and constant-mask smoothing are in the dds directory. In general,
  dds seems to give superior results, though it slightly supresses
  small clusters in favor of larger ones. 3-D performance is very
  similar, 2-D performance is more noticeably superior.
  
### 0.66 ###
- I'm having doubts about percent-space again... ;)
  I think I proved to myself that percent- and density- space don't have to
  behave the same unless there's a linear transformation between them.
  Since I suspect the relationship is highly non-linear, they could vary by
  amounts <= the error caused by a measurement error in each dimension equal
  to the sampling interval. This could be large for higher-dimensional data.
  However, since density space generally has to be visualized on a log scale,
  linear interpolation between sample points probably does not closely approximate
  the underlying density function, and so interpolation between samples in
  percent space is probably closer to the underlying function, giving better
  results.
  (1) I need to verify this somehow, or at least provide evidence.
  (2) We should be graphing in percent space, too.
- comparing 2-D percent space with 2-D density space contours, they are virtually
  indistinguishable -- probably within the floating point error. This approach
  is more than accurate enough! Compare met.kin to met-den.kin

### 0.65 ###
- no-clash data makes significant improvements for Val, Thr, with ~1/6 of data lost.
  Is the loss still tolerable for e.g. Arg?
- for a long time, I was worried about the different scales for percent-space
  and density-space. Having done some comparisons, I'm now satisfied that
  they behave the same for all intents & purposes: contours occur at the same
  x-values, and therefore all points will be classified the same in either scheme.
  However, percent-space allows us to select levels after the fact, and so for
  most of our applications, percent-space is going to be the preferred way to
  represent our data over the long term.
- converted all data storage (.data, .ndft) from RotaND programs to be in
  fraction-excluded format. All density values range from 0.0 (low) to 1.0 (high),
  according to the fraction of the original data that would be excluded by
  a contour at that level. To test if a point falls on/inside the e.g. 95% contour,
  test whether density(x1,x2,...) >= 0.05

  *** All Ramachandran programs will have to be converted before using the new data!

- (10,14) seem to be good smoothing parameters that aren't overfitting the data.
  Some sets, like Met, are just too disperse for tight contours at 95%.
  See "rotamericity" for each residue in the rotamers paper by SC Lovell et al.
  This isn't quite right -- it works for Ile (99%) but not Leu (93%).
- Part of the problem is also that my algorithm assumes a hyper-spherical scatter,
  whereas many of these would in fact be better fit by ellipsoids.

### 0.64 ###
- started revising the rotamers: new 2-D data sets filtered for bad clashes
- converted Rota1D to use a log scale on vertical display
- fixed major bug in level/loglevel for Rota1D where wrong fractions were reported

### 0.63 ###
- work on a suitable scale and (text) format for distribution of Ramachandran data
- proved that for the 2-D versions, you can do direct theoretical scaling using
  pi/(pi^2 - 4) as in the paper and get the same results; but since 3-D and 4-D
  cases are more complicated, I'll keep using normalize().
- started expressing NDFTs as %-style contour levels, such that empty areas
  are 1.0 and high-density areas take on values closer to 0.
- one concern: percent-space and density-space are on different scales, and,
  I suspect, related in a non-trivial way. What results would you get doing
  the linear interpolation in percent-space?
- normalize() now ensures that each point contributed 1.0 units,
  instead of having the contribution depend on bin size.
  Makes the math cleaner in the paper.
- contoured the "no secondary structure" (noSS) data using the new formulas
- discovered bug in Makefile: Gln was done with -limit3 instead of Glu!
- did 1-degree sampled Ramachandran plots for Simon (.data files)

### 0.62 ###
- major rewrite of 2D case: Rota2D
- Rota2D uses kin2Dcont to do contouring
- added Rota3D as well
- added Rota4D and Rota1D

### 0.61 ###
- changed masking scheme to one-parameter version: alpha = k * p^-(lambda/n)
- started exploring 2-D rotamers

### 0.60 ###
- migrated to new build system
- changed value normalization back to normalize()
- New set of parameters for general Rama distribution:
      mask=10,99.95,35,98,15        contour=:99.95,98
- New set of parameters for other Rama distributions:
      mask=10,99.9,46,85,14         contour=:99.8,98
- New levels corresponding to new parameters:
  PLOT         98%     99.8%   99.95%  # data points
  general      1.1062  ---     0.0382  81234
  glycine      0.0875  0.0219  ---      7705 (unique)
  proline      0.1526  0.0473  ---      4415
  pre-pro      0.1254  0.0233  ---      4014
- Levels will be named "favored", "allowed", and "outlier"

### 0.59b ###
- or maybe this is what will be published...
- cosine mask cuts off at 0, where it's steep. This is the wrong thing to do.
- when few data points are excluded, e.g. at 99.95%, the exact height the contour will lie at
  may be ambiguous. It should 'hug' the points and go out no further than necessary.
- Now, how to fix these???
- Fixed tallyCosine functions to have sigmoidal shapes

### 0.58f ###
- New set of parameters for general Rama distribution, decided by Jane and Simon:
      mask=10,99.95,30,98,13        contour=:99.95,98
- New set of parameters for other Rama distributions:
      mask=10,99.9,40,85,12         contour=:99.8,98
- New levels corresponding to new parameters:
  PLOT         98%     99.8%   99.95%  # data points
  general      3.824   ---     0.1299  81234
  glycine      5.932   1.521   ---      7705 (unique)
  proline      13.17   4.290   ---      4415
  pre-pro      31.03   6.128   ---      4014
- Levels will be named "favored", "disfavored", and "forbidden".

### 0.58c ###
- prepared code for "final" form that will generate published contours and be archived
- Jane likes the dd98x version better than the original. Can we find something that's not so wiggly around the edges?

### 0.58b ###
- fitted SH2D with some new options to handle rotamer data
- using bin size = 5 makes very, very little difference vs. size = 2
  2 creates smoother contours in a few places where 5 gives corners,
  but 5 is more than adequate for rotamers.
- In RamPing: made 99.9% buttons more obvious, treat pro-pro as normal pro
- wrote RamaStat to check statistics on the general plot -- resolutions up to 1.8A are all equally good when B < 30

### 0.58a ###
- created Rota2D to do constant-width smoothing of rotamers...
  But this looks *bad* on Tyr/Phe. There really do seem to be 'walls'.
  Not as steep as for rama, but enough that smoothing pushes the edges too far.
- requiring scB < 30 eliminated ~14% of our data, and changed the shape of the
  neck b/t the two major rotamers. However, wall overflow was still a problem.

### 0.57c ###
- removed RamaCheck
- started integrating RamPing into MolProbity

### 0.57b ###
- added standardize() to NDFT to facilitate recording and re-using contour levels
- tables are now scaled to fit the range [0,1000]
- produced what (should be!) final .ndft and .kin files from the various distributions:
    general     98% ->  1.60    99.9% -> 0.137
    gly         98% ->  5.82    99.9% -> 1.03
    pro         98% -> 13.14    99.9% -> 3.27
    prepro      98% -> 35.30    99.9% -> 1.69
    propro      98% -> 13.0     99.9% -> 3.17
- generated a new 'ramacheck-template.kin' based on these results
- replaced RamaCheck with RamPing, which (so far) produces very nice kinemage output
- added HTML table output to RamPing

### 0.56b ###
- selected new contouring values for Gly and Pro
- generated 'ddrama-0.56b.kin' to show new levels vs. various old
- decision taken to contour 'minor' data sets at 98% only, since we can't
  handle lower levels reasonably.
- tried splitting data sets -- combined contour usu. better than average of contours of halves.

### 0.55b ###
- changed to specifying contour levels by the fraction of data points enclosed.
- implemented binary search to discover appropriate contour levels.
- increased mask widths for rama plot: -mask=15,99.94,30,95,7.5
  this eliminates many bumps and wiggles without making contours much worse
  PROCEDURE: contour w/ various constant masks, place lower bound at ~ desired
  lower edge, upper bound along walls. Halve and double masks for DD version.

### 0.54b ###
- added clone() to TabEntry and -sym to SH2D
- added Gly contours to RamaCheck kinemage template
- added Pro contours to RamaCheck kinemage template

### 0.53b ###
- added PostScript output to RamaCheck
- changed SH2D to accept input contour levels as 'per million data points'
  to facilitate comparing different resolutions for the rama plot.
- conclusion: by the time we get to the res<=1.8A level and B<30, nothing changes at lower res

### 0.52b ###
- levels: 0.032 - 0.04 (99.96% - 99.95%); ~ 0.42 (99%); ~ 0.83 (98%); 5 - 7 (5.8 is 90%, all are 'rough')
- at 0.37 you pick up the spot at 55,-130 and the hole at -110,60
  impossible to get two 'monoliths' -- 0.42 is interesting only b/c its at 99%
  3 levels was Jane's idea
- started RamaCheck
- made some changes to TabEntry. These need to be resolved, somehow.

### 0.51a ###
- major rewrite to clean up SmoothHist2D and put more control on the command line
  introduced  -contour=...  and  -mask=...
- added Version.java for version control

### 0.50b ###
- adjusted contour levels to match old regions
- tested B<30 data -- smoothing is (fairly) robust
- iterative smoothing looks like constant mask width -- walls spread back out
- put dd table back on a linear scale

### 0.50a ###
- I am freakin' BRILLIANT!! All the wrapping code in about one hour. Now -- where are the bugs?

### 0.48b ###
- attempted to formalize procedure for determining parameters
- made a large addition to README
- this really looks like it's working!
  This is the first plot produced that Dave and Jane like.
  We're on the right track. Now for some improvements.

### 0.47b ###
- went to 2 degree bin size
- began developing procedure for determining parameters

### 0.46b ###
- fixed a fatal bug in tallyGaussianNorm()
- discovered that reducing mask width for first pass smoothing function
  makes edges less jagged!!

### 0.45a ###
- started working on DumbCont, a simple contouring routine
- DumbCont produces accurate but ugly contours, as desired

### 0.44b ###
- explored using dd-cosine rather than Gaussian
- my first success with dd smoothing!!!
  Use a G20 for determining density, then plot a C[(2,15)->(20,1)], then do a transformLog()
  This preserves the shoals while not smearing the wall.
- kin2Dcont is *too* smart -- its lines don't follow the data along the wall.

### 0.43b ###
- created class MaskSpec; began exploring linearly-dd* masks
(* dd: density dependent)
- substantial reworking of NDFloatTable, including a small change in the file format
  and changes to the tallying functions such that no attempt is made to normalize them.
  Some functions were also renamed.
- fixed tallyCosine() to check radius rather than max()
- added pseudo-normalized tallyCosinePNorm()

### 0.42b ###
- experimented with density-dependent smoothing functions

### 0.41b ###
- produced log traces with diffent masks

### 0.40b ###
- cosine doesn't scale properly
- produced linear traces with different masks

### 0.40a ###
- CHANGELOG initiated
- tallyCosine() added
- transformLog() added
- substantial reorganization of SmoothHist2D; functions were consolidated

==============================================================================
==============================================================================
*/
