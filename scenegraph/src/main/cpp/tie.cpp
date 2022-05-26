#include <stdio.h>

#include "coresection.h"
#include "trackscene.h"
#include "trackscenenode.h"
#include "tie.h"

static SectionTiePoint *inProgressTie = NULL;

SectionTiePoint *get_in_progress_tie() {
    return inProgressTie;
}

bool start_section_tie(int trackId, int sectionId, float x, float y) {
    if (inProgressTie) {
        printf("A tie is already in progress, can't start a new one.\n");
        return false;
    }
    inProgressTie = new SectionTiePoint(trackId, sectionId, x, y);
    return true;
}

CoreSectionTie *finish_section_tie(int trackId, int sectionId, float x, float y) {
    if (!inProgressTie) {
        printf("No tie is in progress, use start_section_tie().\n");
        return NULL;
    }
    SectionTiePoint ptB(trackId, sectionId, x, y);
    CoreSectionTie *tie = new CoreSectionTie(*inProgressTie, ptB);
    delete inProgressTie;
    inProgressTie = NULL;
    return tie;
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

// Convert our section-space coord (this->x, this->y) to scene-space.
void SectionTiePoint::toSceneSpace(float &scenex, float &sceney) {
    TrackSceneNode *t = get_scene_track(track);
    CoreSection *s = get_track_section(t, section);
    scenex = x + t->px + s->px;
    sceney = y + t->py + s->py;
}