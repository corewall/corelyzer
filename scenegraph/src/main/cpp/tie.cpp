#include <stdio.h>

#include "coresection.h"
#include "trackscene.h"
#include "trackscenenode.h"
#include "tie.h"

extern int default_track_scene;

struct EditTieData {
    int id; // ID of existing tie being edited, -1 if new tie
    SectionTieType type;
    SectionTiePoint *fixedPoint;

    static const int NEW_TIE = -1;

    EditTieData() {
        id = NEW_TIE;
        type = NONE;
        fixedPoint = NULL;
    }

    void clear() {
        type = NONE;
        if (id == NEW_TIE) { delete fixedPoint; }
        fixedPoint = NULL;
        id = NEW_TIE;
    }

    void beginEdit(int trackId, int sectionId, SectionTieType type, float fixedX, float fixedY) {
        this->id = NEW_TIE;
        this->type = type;
        this->fixedPoint = new SectionTiePoint(trackId, sectionId, fixedX, fixedY);
    }

    void beginEdit(int tieId, bool fixedPointIsA) {
        this->id = tieId;
        CoreSectionTie *tie = get_tie(default_track_scene, tieId);
        this->type = tie->type;
        fixedPoint = fixedPointIsA ? tie->a : tie->b;
    }

    CoreSectionTie *endEdit(int trackId, int sectionId, float x, float y) {
        CoreSectionTie *newTie = NULL;
        if (this->id == NEW_TIE) { // create new tie
            SectionTiePoint ptB(trackId, sectionId, x, y);
            newTie = new CoreSectionTie(this->type, *(this->fixedPoint), ptB);
        } else { // update point of existing tie
            CoreSectionTie *tie = get_tie(default_track_scene, this->id);
            SectionTiePoint *editedPoint = (tie->a == this->fixedPoint) ? tie->b : tie->a;
            editedPoint->trackId = trackId;
            editedPoint->sectionId = sectionId;
            editedPoint->x = x;
            editedPoint->y = y;
        }
        return newTie;
    }

    bool valid() { return this->type != NONE; }
};

static EditTieData editTieInstance;
static EditTieData *editTie = &editTieInstance;


SectionTiePoint *get_in_progress_tie() {
    if (editTie->valid()) {
        return editTie->fixedPoint;
    }
    return NULL;
}

SectionTieType get_in_progress_tie_type() {
    return editTie->type;
}

void clear_in_progress_tie() {
    if (editTie->valid()) {
        editTie->clear();
    }
}

bool start_section_tie(SectionTieType type, int trackId, int sectionId, float x, float y) {
    if (editTie->valid()) {
        printf("A tie is already in progress, can't start a new one.\n");
        return false;
    }

    editTie->beginEdit(trackId, sectionId, type, x, y);
    return true;
}

bool start_section_tie(int tieId, bool fixedPointIsA) {
    if (editTie->valid()) {
        printf("A tie is already in progress, can't start a new one.\n");
        return false;
    }

    editTie->beginEdit(tieId, fixedPointIsA);
    return true;
}

CoreSectionTie *finish_section_tie(int trackId, int sectionId, float x, float y) {
    if (!editTie->valid()) {
        printf("No tie is in progress, use start_section_tie().\n");
        return NULL;
    }
    CoreSectionTie *newTie = editTie->endEdit(trackId, sectionId, x, y);
    clear_in_progress_tie();
    return newTie;
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
    TrackSceneNode *t = get_scene_track(trackId);
    CoreSection *s = get_track_section(t, sectionId);
    scenex = x + t->px + s->px;
    sceney = y + t->py + s->py;
}

// Do the parent track and section of this SectionTiePoint exist?
bool SectionTiePoint::valid() {
    bool valid = false;
    TrackSceneNode *t = get_scene_track(trackId);
    if (t) {
        CoreSection *s = get_track_section(t, sectionId);
        valid = (s != NULL);
    }
    return valid;
}