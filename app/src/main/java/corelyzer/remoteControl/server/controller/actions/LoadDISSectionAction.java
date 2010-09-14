package corelyzer.remoteControl.server.controller.actions;

import corelyzer.controller.CRExperimentController;
import corelyzer.graphics.SceneGraph;

public class LoadDISSectionAction extends AbstractAction {
	protected final String expedition;
	protected final String site;
	protected final String hole;
	protected final String core;
	protected final String section;
	protected final String url;
	protected final float top;
	protected final float length;
	protected final float topOffset;
	protected final float bottomOffset;

	public LoadDISSectionAction(final String expedition, final String site, final String hole, final String core, final String section, final String url,
			final float top, final float length, final float topOffset, final float bottomOffset) {
		super(new String[0]); // JOSH: do we really need this?
		this.expedition = expedition;
		this.site = site;
		this.hole = hole;
		this.core = core;
		this.section = section;
		this.url = url;
		this.top = top;
		this.length = length;
		this.topOffset = topOffset;
		this.bottomOffset = bottomOffset;
	}

	// @Override

	public void run() {
		float adjustedTop = top;
		float adjustedLength = length;
		if (topOffset > 0) {
			adjustedTop -= topOffset;
			adjustedLength += topOffset;
		}
		if (bottomOffset > 0) {
			adjustedLength += bottomOffset;
		}

		// load the image
		String sectionName = url.substring(url.lastIndexOf('/') + 1);
		CRExperimentController.loadSectionImageWithLength(url, sectionName, expedition + "_" + site, hole + "_" + core, adjustedTop, adjustedLength);
		int[] id = CRExperimentController.getSectionLocationWithURL(url);
		SceneGraph.setSectionIntervalTop(id[0], id[1], topOffset);
		SceneGraph.setSectionIntervalBottom(id[0], id[1], (adjustedLength - bottomOffset) * 100);
	}
}
