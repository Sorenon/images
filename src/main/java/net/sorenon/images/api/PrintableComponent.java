package net.sorenon.images.api;

import dev.onyxstudios.cca.api.v3.component.Component;

public interface PrintableComponent extends Component {

    Print getPrint();

    boolean setPrint(Print print);
}
