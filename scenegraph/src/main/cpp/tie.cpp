#include <stdio.h>

#include "coresection.h"
#include "trackscene.h"
#include "trackscenenode.h"
#include "tie.h"

extern int default_track_scene;

class SectionTieEditData {
    public:
        virtual ~SectionTieEditData() {}
        virtual SectionTieType getType() = 0;
        virtual SectionTiePoint *getFixedPoint() = 0;
        virtual CoreSectionTie *endEdit(int trackId, int sectionId, float x, float y) = 0;
};

class NewTieEditData : SectionTieEditData {
    SectionTieType type;
    SectionTiePoint *fixedPoint;

public:
    NewTieEditData(int trackId, int sectionId, SectionTieType type, float fixedX, float fixedY) {
        this->type = type;
        this->fixedPoint = new SectionTiePoint(trackId, sectionId, fixedX, fixedY);
    }
    virtual ~NewTieEditData() {
        if (this->fixedPoint) { delete this->fixedPoint; }
    }
    virtual SectionTieType getType() { return this->type; }
    virtual SectionTiePoint *getFixedPoint() { return this->fixedPoint; }
    virtual CoreSectionTie *endEdit(int trackId, int sectionId, float x, float y) {
        SectionTiePoint ptB(trackId, sectionId, x, y);
        return new CoreSectionTie(this->type, *(this->fixedPoint), ptB);
    }
};

class ExistingTieEditData : SectionTieEditData {
    int tieId;
    SectionTieType type;
    SectionTiePoint *fixedPoint;

public:
    ExistingTieEditData(int tieId, bool fixedPointIsA) {
        this->tieId = tieId;
        CoreSectionTie *tie = get_tie(default_track_scene, tieId);
        this->type = tie->type;
        this->fixedPoint = fixedPointIsA ? tie->a : tie->b;
    }
    virtual ~ExistingTieEditData() {}
    virtual SectionTieType getType() { return this->type; }
    virtual SectionTiePoint *getFixedPoint() { return this->fixedPoint; }
    virtual CoreSectionTie *endEdit(int trackId, int sectionId, float x, float y) {
        CoreSectionTie *tie = get_tie(default_track_scene, this->tieId);
        SectionTiePoint *editedPoint = (tie->a == this->fixedPoint) ? tie->b : tie->a;
        editedPoint->trackId = trackId;
        editedPoint->sectionId = sectionId;
        editedPoint->x = x;
        editedPoint->y = y;
        return NULL;
    }
};

static SectionTieEditData *editTie = NULL;


bool is_editing_tie() { return editTie != NULL; }

SectionTiePoint *get_edit_tie_fixed_point() {
    if (editTie) {
        return editTie->getFixedPoint();
    }
    return NULL;
}

SectionTieType get_edit_tie_type() {
    if (editTie) {
        return editTie->getType();
    }
    return NONE;
}

void cleanup_edit_tie() {
    if (editTie) {
        delete editTie;
        editTie = NULL;
    }
}

bool start_edit_new_tie(SectionTieType type, int trackId, int sectionId, float x, float y) {
    if (editTie) {
        printf("A tie is already being edited.\n");
        return false;
    }

    editTie = (SectionTieEditData *)new NewTieEditData(trackId, sectionId, type, x, y);
    return true;
}

bool start_edit_existing_tie(int tieId, bool fixedPointIsA) {
    if (editTie) {
        printf("A tie is already being edited.\n");
        return false;
    }

    editTie = (SectionTieEditData *)new ExistingTieEditData(tieId, fixedPointIsA);
    return true;
}

CoreSectionTie *commit_edit_tie(int trackId, int sectionId, float x, float y) {
    if (!editTie) {
        printf("No edit tie to finish.\n");
        return NULL;
    }
    CoreSectionTie *newTie = editTie->endEdit(trackId, sectionId, x, y);
    cleanup_edit_tie();
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