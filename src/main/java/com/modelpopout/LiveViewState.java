package com.modelpopout;

/** EDT-confined latest model/view state: one active render and one replaceable latest request. */
final class LiveViewState
{
    private ModelSnapshot model;
    private TextureSet textures = TextureSet.EMPTY;
    private double texturePhase;
    private ModelFrame framing;
    private ModelSnapshot npcModel;
    private ModelFrame npcFraming;
    private long npcKey = -1, sceneSession;
    private boolean npcEnabled;
    private CameraView camera = CameraView.DEFAULT;
    private long revision, modelSession, attemptedRevision = -1;
    private RenderRequest active;

    boolean updateModel(ModelSnapshot next, boolean force)
    {
        if (!force && next.sameAppearance(model)) return false;
        if (framing == null) framing = ModelFrame.initial(next);
        model = next;
        revision++;
        return true;
    }

    boolean updateNpc(ModelSnapshot next, long key)
    {
        if (!npcEnabled) { next = null; key = -1; }
        boolean identityChanged = key != npcKey;
        boolean visibilityChanged = (next != null) != (npcModel != null);
        if (!identityChanged && !visibilityChanged && (next == null || next.sameAppearance(npcModel))) return false;
        if (identityChanged || visibilityChanged) sceneSession++;
        if (identityChanged) npcFraming = null;
        if (next != null && npcFraming == null) npcFraming = ModelFrame.initial(next);
        npcKey = key;
        npcModel = next;
        revision++;
        return true;
    }

    boolean removeNpc(long key)
    {
        return npcKey == key && updateNpc(null, -1);
    }

    void setNpcEnabled(boolean enabled)
    {
        if (npcEnabled == enabled) return;
        npcEnabled = enabled;
        if (!enabled) { npcModel = null; npcFraming = null; npcKey = -1; }
        sceneSession++;
        revision++;
    }

    boolean npcEnabled() { return npcEnabled; }
    boolean hasNpc() { return npcEnabled && npcModel != null; }
    long npcKey() { return npcKey; }

    CameraView camera()
    {
        return camera;
    }

    void setCamera(CameraView next)
    {
        if (camera.yaw == next.yaw && camera.pitch == next.pitch && camera.zoom == next.zoom) return;
        camera = next;
        revision++;
    }

    boolean updateTextures(TextureSet next)
    {
        if (next.samePixels(textures)) return false;
        textures = next;
        revision++;
        return true;
    }

    boolean hasAnimatedTextures() { return textures.animated(); }

    void setTexturePhase(double phase)
    {
        double quantized = Math.floor(phase*1024)/1024;
        if (textures.animated() && quantized != texturePhase) { texturePhase = quantized; revision++; }
    }

    boolean needsRender() { return active == null && model != null && revision != attemptedRevision; }

    RenderRequest begin()
    {
        if (active != null || model == null || revision == attemptedRevision) return null;
        active = new RenderRequest(model, camera, framing, revision, modelSession,
            npcEnabled, npcModel, npcFraming, npcKey, sceneSession,textures,texturePhase);
        return active;
    }

    boolean finish(RenderRequest request)
    {
        if (active != request) return false;
        active = null;
        attemptedRevision = request.revision;
        return valid(request);
    }

    boolean valid(RenderRequest request)
    {
        return request.modelSession == modelSession && request.sceneSession == sceneSession;
    }

    void clear()
    {
        model = null;
        textures = TextureSet.EMPTY;
        texturePhase = 0;
        npcModel = null; npcFraming = null; npcKey = -1;
        sceneSession++;
        modelSession++;
        revision++;
        // Keep the active slot occupied until the real worker ends, including across close/reopen.
    }

    void resetFraming()
    {
        clear();
        framing = null;
    }

    static final class RenderRequest
    {
        final TextureSet textures;
        final double texturePhase;
        final ModelSnapshot model;
        final CameraView camera;
        final ModelFrame framing, npcFraming;
        final ModelSnapshot npcModel;
        final boolean npcEnabled;
        final long npcKey;
        private final long revision, modelSession, sceneSession;
        private RenderRequest(ModelSnapshot model, CameraView camera, ModelFrame framing, long revision, long modelSession,
            boolean npcEnabled, ModelSnapshot npcModel, ModelFrame npcFraming, long npcKey, long sceneSession,TextureSet textures,double texturePhase)
        {
            this.textures = textures; this.texturePhase = texturePhase;
            this.model = model;
            this.camera = camera;
            this.framing = framing;
            this.revision = revision;
            this.modelSession = modelSession;
            this.npcEnabled = npcEnabled; this.npcModel = npcModel; this.npcFraming = npcFraming;
            this.npcKey = npcKey; this.sceneSession = sceneSession;
        }
    }
}
