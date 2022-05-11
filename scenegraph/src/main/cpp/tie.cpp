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
    tie->setDestination(trackId, sectionId, x, y);
    return true;
}

void CoreSectionTie::setDestination(int trackId, int coreId, float x, float y) {
    destTrack = trackId;
    destCore = coreId;
    ax = x;
    ay = y;
    complete = true;
}

void CoreSectionTie::setSourceDescription(char *desc) { srcDesc = desc; }
void CoreSectionTie::setDestinationDescription(char *desc) { destDesc = desc; }
char *CoreSectionTie::getSourceDescription() { return srcDesc; }
char *CoreSectionTie::getDestinationDescription() { return destDesc; }
bool CoreSectionTie::getShow() { return show; }
void CoreSectionTie::setShow(bool _show) { show = _show; }