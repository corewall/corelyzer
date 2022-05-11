#include <stdlib.h>
#include <stdio.h>

#include "tie.h"

static CoreSectionTie *activeTie = NULL;

CoreSectionTie *get_active_tie() {
     return activeTie;
}

void set_active_tie(CoreSectionTie *tie) {
    activeTie = tie;
}

//================================================================
CoreSectionTie* create_section_tie(int type, int trackId, int sectionId, float x, float y) {
    printf("Starting tie: track %d core %d\n", trackId, sectionId);
    CoreSectionTie *tie = new CoreSectionTie(type, trackId, sectionId, x, y);
    return tie;
}

//================================================================
bool finish_section_tie(CoreSectionTie *tie, int trackId, int sectionId, float x, float y) {
    printf("Finishing tie: track %d core %d\n", trackId, sectionId);
    tie->setDestination(trackId, sectionId, x, y);
    return true;
}

void render_tie(CoreSectionTie *t) {
    return;
}