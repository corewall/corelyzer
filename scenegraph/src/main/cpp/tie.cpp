#include <stdio.h>

#include "tie.h"

static CoreSectionTie *activeTie = NULL; // in-progress tie with one endpoint

CoreSectionTie *get_active_tie() {
     return activeTie;
}

void set_active_tie(CoreSectionTie *tie) {
    activeTie = tie;
}

// Create a CoreSectionTie on the specified track/section with source endpoint at (x,y).
// Client is responsible for freeing.
CoreSectionTie* create_section_tie(int type, int trackId, int sectionId, float x, float y) {
    printf("Starting tie: track %d core %d\n", trackId, sectionId);
    CoreSectionTie *tie = new CoreSectionTie(type, trackId, sectionId, x, y);
    return tie;
}

// Set the track, section, and dest(ination) endpoint of the specified CoreSectionTie at (x,y).
bool finish_section_tie(CoreSectionTie *tie, int trackId, int sectionId, float x, float y) {
    printf("Finishing tie: track %d core %d\n", trackId, sectionId);
    tie->setB(trackId, sectionId, x, y);
    return true;
}

void CoreSectionTie::setB(int trackId, int coreId, float x, float y) {
    bTrack = trackId;
    bCore = coreId;
    bx = x;
    by = y;
    complete = true;
}

void CoreSectionTie::setADescription(char *desc) {
    if (aDesc) delete[] aDesc;
    aDesc = desc;
}
void CoreSectionTie::setBDescription(char *desc) {
    if (bDesc) delete[] bDesc;
    bDesc = desc;
}
char *CoreSectionTie::getADescription() { return aDesc; }
char *CoreSectionTie::getBDescription() { return bDesc; }
bool CoreSectionTie::getShow() { return show; }
void CoreSectionTie::setShow(bool _show) { show = _show; }