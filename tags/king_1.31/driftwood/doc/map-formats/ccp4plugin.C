/***************************************************************************
 *cr
 *cr            (C) Copyright 1995-2003 The Board of Trustees of the
 *cr                        University of Illinois
 *cr                         All Rights Reserved
 *cr
 ***************************************************************************/

/***************************************************************************
 * RCS INFORMATION:
 *
 *      $RCSfile: ccp4plugin.C,v $
 *      $Author: eamon $       $Locker:  $             $State: Exp $
 *      $Revision: 1.8 $       $Date: 2003/05/23 18:15:38 $
 *
 ***************************************************************************/

/*
 * CCP4 electron density map file format: http://www.ccp4.ac.uk/html/maplib.html
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>

#include "molfile_plugin.h"

// swap ndata 4-byte words
static void swap4(void *data, int ndata) {
  int i;
  char *dataptr;
  char tmp;

  dataptr = (char *) data;
  for (i=0; i<ndata; i++) {
    tmp = dataptr[0];
    dataptr[0] = dataptr[3];
    dataptr[3] = tmp;
    tmp = dataptr[1];
    dataptr[1] = dataptr[2];
    dataptr[2] = tmp;
    dataptr += 4;
  }
}


typedef struct {
  FILE *fd;
  int nsets;
  int swap;
  int xyz2crs[3];
  long dataOffset;
  molfile_volumetric_t *vol;
} ccp4_t;

static void *open_ccp4_read(const char *filepath, const char *filetype,
    int *natoms) {
  FILE *fd;
  ccp4_t *ccp4;
  char mapString[4], symData[81];
  int origin[3], extent[3], grid[3], crs2xyz[3], mode, symBytes;
  int swap, i, xIndex, yIndex, zIndex;
  float cellDimensions[3], cellAngles[3];
  float alpha, beta, gamma, xScale, yScale, zScale;
  
  fd = fopen(filepath, "rb");
  if (!fd) 
    return NULL;

  if ( (fread(extent, sizeof(int), 3, fd) != 3) ||
       (fread(&mode, sizeof(int), 1, fd) != 1) ||
       (fread(origin, sizeof(int), 3, fd) != 3) ||
       (fread(grid, sizeof(int), 3, fd) != 3) ||
       (fread(cellDimensions, sizeof(float), 3, fd) != 3) ||
       (fread(cellAngles, sizeof(float), 3, fd) != 3) ||
       (fread(crs2xyz, sizeof(int), 3, fd) != 3) )
  {
    fprintf(stderr, "Problem reading the file.\n");
    return NULL;
  }

  // Check the number of bytes used for storing symmetry operators
  fseek(fd, 92, SEEK_SET);
  if ( (fread(&symBytes, sizeof(int), 1, fd) != 1) )
  {
    fprintf(stderr, "Problem reading the file.\n");
    return NULL;
  }

  // Check for the string "MAP" at byte 208, indicating a CCP4 file.
  fseek(fd, 208, SEEK_SET);
  if ( (fgets(mapString, 4, fd) == NULL) ||
       (strcmp(mapString, "MAP") != 0) )
  {
    fprintf(stderr, "File not in CCP4 format.\n");
    return NULL;
  }

  swap = 0;
  // Check the data type of the file.
  if (mode != 2)
  {
    // Check if the byte-order is flipped
    swap4(&mode, 1);
    if (mode != 2)
    {
      fprintf(stderr, "Non-real data types are currently not supported.\n");
      return NULL;
    }
    else
      swap = 1;
  }

  // Swap all the information obtained from the header
  if (swap == 1)
  {
    swap4(extent, 3);
    swap4(origin, 3);
    swap4(grid, 3);
    swap4(cellDimensions, 3);
    swap4(cellAngles, 3);
    swap4(crs2xyz, 3);
    swap4(&symBytes, 1);
  }
  
  // Read symmetry records -- organized as 80-byte lines of text.
  if (symBytes != 0)
  {
    fprintf(stdout, "Symmetry records found:\n");
    fseek(fd, 1024, SEEK_SET);
    for (i = 0; i < symBytes/80; i++)
    {
      fgets(symData, 81, fd);
      fprintf(stdout, "%s\n", symData);
    }
  }

  xScale = cellDimensions[0] / grid[0];
  yScale = cellDimensions[1] / grid[1];
  zScale = cellDimensions[2] / grid[2];

  // Allocate and initialize the ccp4 structure
  ccp4 = new ccp4_t;
  ccp4->fd = fd;
  ccp4->vol = NULL;
  *natoms = MOLFILE_NUMATOMS_NONE;
  ccp4->nsets = 1; // this EDM file contains only one data set
  ccp4->swap = swap;
  ccp4->dataOffset = 1024 + symBytes;
  
  ccp4->vol = new molfile_volumetric_t[1];
  strcpy(ccp4->vol[0].dataname, "CCP4 Electron Density Map");

  // Mapping between CCP4 column, row, section and VMD x, y, z.
  ccp4->xyz2crs[crs2xyz[0]-1] = 0;
  ccp4->xyz2crs[crs2xyz[1]-1] = 1;
  ccp4->xyz2crs[crs2xyz[2]-1] = 2;
  xIndex = ccp4->xyz2crs[0];
  yIndex = ccp4->xyz2crs[1];
  zIndex = ccp4->xyz2crs[2];

  ccp4->vol[0].origin[0] = xScale * origin[xIndex];
  ccp4->vol[0].origin[1] = yScale * origin[yIndex];
  ccp4->vol[0].origin[2] = zScale * origin[zIndex];

  // calculate non-orthogonal unit cell coordinates
  alpha = (3.14159265358979323846 / 180.0) * cellAngles[0];
  beta = (3.14159265358979323846 / 180.0) * cellAngles[1];
  gamma = (3.14159265358979323846 / 180.0) * cellAngles[2];

  ccp4->vol[0].xaxis[0] = xScale * extent[xIndex];
  ccp4->vol[0].xaxis[1] = 0;
  ccp4->vol[0].xaxis[2] = 0;

  ccp4->vol[0].yaxis[0] = cos(gamma) * xScale * extent[xIndex];
  ccp4->vol[0].yaxis[1] = sin(gamma) * yScale * extent[yIndex];
  ccp4->vol[0].yaxis[2] = 0;

  float z1, z2, z3;
  z1 = cos(beta);
  z2 = (cos(alpha) - cos(beta)*cos(gamma)) / sin(gamma);
  z3 = sqrt(1.0 - z1*z1 - z2*z2);
  ccp4->vol[0].zaxis[0] = z1 * xScale * extent[xIndex];
  ccp4->vol[0].zaxis[1] = z2 * yScale * extent[yIndex];
  ccp4->vol[0].zaxis[2] = z3 * zScale * extent[zIndex];

  ccp4->vol[0].xsize = extent[xIndex];
  ccp4->vol[0].ysize = extent[yIndex];
  ccp4->vol[0].zsize = extent[zIndex];

  ccp4->vol[0].has_color = 0;

  return ccp4;
}

static int read_ccp4_metadata(void *v, int *nsets, 
  molfile_volumetric_t **metadata) {
  ccp4_t *ccp4 = (ccp4_t *)v;
  *nsets = ccp4->nsets; 
  *metadata = ccp4->vol;  

  return MOLFILE_SUCCESS;
}

static int read_ccp4_data(void *v, int set, float *datablock,
                         float *colorblock) {
  ccp4_t *ccp4 = (ccp4_t *)v;
  float currDensity, *cell = datablock;
  int x, y, z, xSize, ySize, zSize, xySize, extent[3], coord[3];
  FILE *fd = ccp4->fd;

  xSize = ccp4->vol[0].xsize;
  ySize = ccp4->vol[0].ysize;
  zSize = ccp4->vol[0].zsize;
  xySize = xSize * ySize;

  // coord = <col, row, sec>
  // extent = <colSize, rowSize, secSize>
  extent[ccp4->xyz2crs[0]] = xSize;
  extent[ccp4->xyz2crs[1]] = ySize;
  extent[ccp4->xyz2crs[2]] = zSize;

  fseek(fd, ccp4->dataOffset, SEEK_SET);

  for (coord[2] = 0; coord[2] < extent[2]; coord[2]++)
  {
    for (coord[1] = 0; coord[1] < extent[1]; coord[1]++)
    {
      for (coord[0] = 0; coord[0] < extent[0]; coord[0]++)
      {
        if (feof(fd) || ferror(fd))
        {
          fprintf(stderr, "Problem reading the file.\n");
          return MOLFILE_ERROR;
        }

        x = coord[ccp4->xyz2crs[0]];
        y = coord[ccp4->xyz2crs[1]];
        z = coord[ccp4->xyz2crs[2]];
        fread(&currDensity, sizeof(float), 1, fd);
        if (ccp4->swap == 1)
          swap4(&currDensity, 1);
        cell[x + y*xSize + z*xySize] = currDensity;
      }
    }
  }

  return MOLFILE_SUCCESS;
}

static void close_ccp4_read(void *v) {
  ccp4_t *ccp4 = (ccp4_t *)v;

  fclose(ccp4->fd);
  delete [] ccp4->vol; 
  delete ccp4;
}

/*
 * Initialization stuff here
 */
static molfile_plugin_t plugin = {
  vmdplugin_ABIVERSION,   // ABI version
  MOLFILE_PLUGIN_TYPE, 	  // plugin type
  "ccp4",                 // file format description
  "Eamon Caddigan",       // author(s)
  0,                      // major version
  1,                      // minor version
  VMDPLUGIN_THREADSAFE,   // is reentrant
  "ccp4"                  // filename extension
};

int VMDPLUGIN_init(void) { return VMDPLUGIN_SUCCESS; }
int VMDPLUGIN_fini(void) { return VMDPLUGIN_SUCCESS; }
int VMDPLUGIN_register(void *v, vmdplugin_register_cb cb) {
  plugin.open_file_read = open_ccp4_read;
  plugin.read_volumetric_metadata = read_ccp4_metadata;
  plugin.read_volumetric_data = read_ccp4_data;
  plugin.close_file_read = close_ccp4_read;
  (*cb)(v, (vmdplugin_t *)&plugin);
  return VMDPLUGIN_SUCCESS;
}

